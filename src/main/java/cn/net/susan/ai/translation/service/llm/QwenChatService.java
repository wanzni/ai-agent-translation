package cn.net.susan.ai.translation.service.llm;

import cn.net.susan.ai.translation.config.DashscopeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 DashScope Chat Completions 生成“客服”英文回复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenChatService {

    private final DashscopeProperties properties;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 依据用户英文消息，生成客服英文回复。
     * @param userEnglishText 用户英文文本（若非英文，建议传入已翻译的英文）
     * @return 客服英文回复文本
     */
    public String replyAsCustomerService(String userEnglishText) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key 未配置，请在 ai.dashscope.api-key 中设置");
        }

        String systemPrompt = "You are a professional customer service agent. " +
                "Always be polite, concise, and helpful. Reply in English only.";
        String userPrompt = "User message (English):\n\n" + userEnglishText + "\n\n" +
                "Please respond as a customer service representative.";

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (properties.getTemperature() != null) {
            body.put("temperature", properties.getTemperature());
        }

        String url = properties.getBaseUrl();

        try {
            Map<String, Object> resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 4xx 响应(客服): status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 4xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 5xx 响应(客服): status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 5xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .bodyToMono(Map.class)
                    .block();

            return extractText(resp);
        } catch (Exception e) {
            log.error("生成客服回复失败: {}", e.getMessage(), e);
            // 失败时给出保底回复，确保用户体验
            return "Hello, thank you for your message. We will assist you shortly.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> resp) {
        if (resp == null) return "";
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> first = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) first.get("message");
                Object content = message != null ? message.get("content") : null;
                return content != null ? String.valueOf(content) : "";
            }
        } catch (Exception ignore) {}
        return "";
    }
}