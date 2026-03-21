package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.core.agent.TranslationAgent;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.service.sse.SseStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final TranslationAgent translationAgent;
    private final SseStreamService sseStreamService;
    private final ObjectMapper objectMapper;

    @PostMapping("/translate")
    public AgentResponse translate(@RequestBody TranslationRequest request) throws Exception {
        log.info("收到Agent翻译请求: {} -> {}", request.getSourceLanguage(), request.getTargetLanguage());

        TranslationAgent.AgentResponse response = translationAgent.think(request);

        return new AgentResponse(
                response.thought(),
                response.action(),
                response.observation(),
                response.finalResponse(),
                response.toolCalls(),
                response.totalDurationMs()
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTranslate(
            @RequestParam("sourceText") String sourceText,
            @RequestParam("sourceLanguage") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "domain", required = false) String domain) {

        SseEmitter emitter = new SseEmitter(60_000L);

        emitter.onError(ex -> log.warn("Agent SSE连接错误: {}", ex.getMessage()));
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("timeout"));
                emitter.complete();
            } catch (Exception ignore) {}
        });

        CompletableFuture.runAsync(() -> {
            try {
                TranslationRequest request = TranslationRequest.builder()
                        .sourceText(sourceText)
                        .sourceLanguage(sourceLanguage)
                        .targetLanguage(targetLanguage)
                        .domain(domain)
                        .useTerminology(true)
                        .useRag(true)
                        .build();

                emitter.send(SseEmitter.event().name("start")
                        .data("{\"status\":\"Agent开始推理...\"}"));

                TranslationAgent.AgentResponse response = translationAgent.think(request);

                if (response.thought() != null && !response.thought().isEmpty()) {
                    String thoughtData = objectMapper.writeValueAsString(
                            new StepData("thought", response.thought()));
                    emitter.send(SseEmitter.event().name("thought").data(thoughtData));
                    Thread.sleep(500);
                }

                if (response.action() != null && !response.action().isEmpty() && !response.action().equals("null")) {
                    String actionData = objectMapper.writeValueAsString(
                            new StepData("action", response.action()));
                    emitter.send(SseEmitter.event().name("action").data(actionData));
                    Thread.sleep(300);
                }

                if (response.observation() != null && !response.observation().isEmpty()) {
                    String obsData = objectMapper.writeValueAsString(
                            new StepData("observation", response.observation()));
                    emitter.send(SseEmitter.event().name("observation").data(obsData));
                    Thread.sleep(300);
                }

                String finalText = response.finalResponse();
                if (finalText == null) finalText = "";

                emitter.send(SseEmitter.event().name("translating")
                        .data("{\"status\":\"翻译中...\",\"text\":\"\"}"));

                int chunkSize = sourceLanguage != null && sourceLanguage.startsWith("en") ? 8 : 16;
                long delayMs = sourceLanguage != null && sourceLanguage.startsWith("en") ? 80L : 40L;

                StringBuilder translated = new StringBuilder();
                for (int i = 0; i < finalText.length(); i += chunkSize) {
                    String chunk = finalText.substring(i, Math.min(finalText.length(), i + chunkSize));
                    translated.append(chunk);
                    String chunkData = objectMapper.writeValueAsString(
                            new ChunkData("delta", chunk, translated.toString()));
                    emitter.send(SseEmitter.event().name("delta").data(chunkData));
                    Thread.sleep(delayMs);
                }

                String doneData = objectMapper.writeValueAsString(
                        new DoneData("done", translated.toString(), response.totalDurationMs()));
                emitter.send(SseEmitter.event().name("done").data(doneData));

                emitter.complete();

            } catch (Exception e) {
                log.error("Agent流式翻译失败: {}", e.getMessage(), e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignore) {}
            }
        });

        return emitter;
    }

    private record StepData(String step, String content) {}
    private record ChunkData(String event, String chunk, String accumulated) {}
    private record DoneData(String event, String translatedText, long durationMs) {}

    public record AgentResponse(
            String thought,
            String action,
            String observation,
            String finalResponse,
            java.util.List<TranslationAgent.AgentResponse.ToolCall> toolCalls,
            long totalDurationMs
    ) {}
}