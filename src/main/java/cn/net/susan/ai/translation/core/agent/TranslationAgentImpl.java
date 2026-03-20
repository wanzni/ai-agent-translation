package cn.net.susan.ai.translation.core.agent;

import cn.net.susan.ai.translation.config.DashscopeProperties;
import cn.net.susan.ai.translation.core.agent.TranslationAgent.AgentResponse.ToolCall;
import cn.net.susan.ai.translation.dto.RagContext;
import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.service.TranslationService;
import cn.net.susan.ai.translation.service.llm.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationAgentImpl implements TranslationAgent {

    private final ToolRegistry toolRegistry;
    private final TranslationService translationService;
    private final RagService ragService;
    private final DashscopeProperties dashscopeProperties;
    private final WebClient webClient = WebClient.builder().build();

    private static final String REACT_SYSTEM_PROMPT = """
            You are an AI Translation Agent with access to tools.

            Available tools:
            - terminology_search: Search for terminology translations
            - translate: Execute the actual translation
            - search_history: Search for similar historical translations

            Follow the ReAct pattern:
            1. Thought: Analyze the translation request
            2. Action: Call appropriate tools if needed (e.g., terminology_search)
            3. Observation: Get the tool results
            4. Final: Provide the final translation

            Always respond in valid JSON format:
            {
                "thought": "your thinking process",
                "action": "tool_name or null",
                "actionParams": {"param": "value"} or null,
                "observation": "tool result or null",
                "final": "final translation or null"
            }
            """;

    @Override
    public AgentResponse think(TranslationRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        List<ToolCall> toolCalls = new ArrayList<>();
        String thought = "";
        String action = "";
        String observation = "";
        String finalResponse = "";

        try {
            RagContext ragContext = ragService.buildRagContext(request);
            String ragContextStr = buildRagContextString(ragContext);

            String userPrompt = buildUserPrompt(request, ragContextStr);
            String llmResponse = callLlm(REACT_SYSTEM_PROMPT, userPrompt);

            ParseResult parsed = parseLlmResponse(llmResponse);
            thought = parsed.thought != null ? parsed.thought : "";
            action = parsed.action != null ? parsed.action : "";
            observation = parsed.observation != null ? parsed.observation : "";

            if (action != null && !action.isEmpty() && !action.equals("null")) {
                Map<String, Object> params = parsed.actionParams != null ? parsed.actionParams : Map.of();
                Tool.ToolResult toolResult = toolRegistry.execute(action, params);
                observation = toolResult.output();
                toolCalls.add(new ToolCall(action, params, observation));

                if ("terminology_search".equals(action) && toolResult.success()) {
                    request.setRagContext(Map.of("terminology", observation));
                }
            }

            TranslationResponse translationResult = translationService.translate(request);
            finalResponse = translationResult.getTranslatedText();

        } catch (Exception e) {
            log.error("Agent thinking failed: {}", e.getMessage(), e);
            finalResponse = "Translation failed: " + e.getMessage();
        }

        long duration = System.currentTimeMillis() - startTime;

        log.info("========== Agent ReAct 推理过程 ==========");
        log.info("Thought: {}", thought);
        log.info("Action: {}", action);
        log.info("Observation: {}", observation);
        log.info("ToolCalls: {}", toolCalls.size());
        for (ToolCall tc : toolCalls) {
            log.info("  - Tool: {}, Params: {}, Result: {}",
                    tc.toolName(), tc.params(), tc.result());
        }
        log.info("Final Response: {}", finalResponse);
        log.info("Total Duration: {}ms", duration);
        log.info("==========================================");

        return new AgentResponse(thought, action, observation, finalResponse, toolCalls, duration);
    }

    private String buildRagContextString(RagContext ragContext) {
        if (ragContext == null) return "";

        StringBuilder sb = new StringBuilder();

        if (ragContext.getGlossaryMap() != null && !ragContext.getGlossaryMap().isEmpty()) {
            sb.append("Terminology constraints:\n");
            ragContext.getGlossaryMap().forEach((k, v) ->
                    sb.append("  ").append(k).append(" -> ").append(v).append("\n"));
        }

        if (ragContext.getHistorySnippets() != null && !ragContext.getHistorySnippets().isEmpty()) {
            sb.append("Historical translations:\n");
            for (RagContext.HistorySnippet snippet : ragContext.getHistorySnippets()) {
                sb.append("  SRC: ").append(snippet.getSource()).append("\n");
                sb.append("  TGT: ").append(snippet.getTarget()).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildUserPrompt(TranslationRequest request, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Translate the following text:\n\n");
        prompt.append("Source text: ").append(request.getSourceText()).append("\n");
        prompt.append("Source language: ").append(request.getSourceLanguage()).append("\n");
        prompt.append("Target language: ").append(request.getTargetLanguage()).append("\n");

        if (request.getDomain() != null) {
            prompt.append("Domain: ").append(request.getDomain()).append("\n");
        }

        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append("\nContext from knowledge base:\n").append(ragContext);
        }

        return prompt.toString();
    }

    private String callLlm(String systemPrompt, String userPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", dashscopeProperties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (dashscopeProperties.getTemperature() != null) {
            body.put("temperature", dashscopeProperties.getTemperature());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(dashscopeProperties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + dashscopeProperties.getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractTextFromResponse(response);
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            return "{\"thought\":\"LLM call failed\",\"action\":null,\"actionParams\":null,\"observation\":null,\"final\":null}";
        }
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        if (response == null) return "";
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> first = choices.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) first.get("message");
                Object content = message != null ? message.get("content") : null;
                return content != null ? String.valueOf(content) : "";
            }
        } catch (Exception e) {
            log.warn("Failed to extract text from LLM response: {}", e.getMessage());
        }
        return "";
    }

    private ParseResult parseLlmResponse(String response) {
        try {
            String thought = extractJsonValue(response, "thought");
            String action = extractJsonValue(response, "action");
            String observation = extractJsonValue(response, "observation");

            return new ParseResult(thought, action, null, observation);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return new ParseResult("Parse error", null, null, null);
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            int keyStart = json.indexOf("\"" + key + "\":");
            if (keyStart == -1) return null;

            int valueStart = json.indexOf("\"", keyStart + key.length() + 2);
            if (valueStart == -1) return null;

            int valueEnd = json.indexOf("\"", valueStart + 1);
            if (valueEnd == -1) return null;

            return json.substring(valueStart + 1, valueEnd);
        } catch (Exception e) {
            return null;
        }
    }

    private record ParseResult(String thought, String action, Map<String, Object> actionParams, String observation) {
    }
}