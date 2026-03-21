package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.core.agent.TranslationAgent;
import cn.net.wanzni.ai.translation.dto.*;
import cn.net.wanzni.ai.translation.service.TranslationService;
import cn.net.wanzni.ai.translation.service.PolishService;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.sse.SseStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 翻译API控制器
 * 
 * 提供翻译相关的RESTful API接口
 * 
 * @version 1.0.0
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
@Validated
public class TranslationController {

    private final TranslationService translationService;
    private final PolishService polishService;
    private final TranslationAgent translationAgent;
    private final SseStreamService sseStreamService;
    private final ObjectMapper objectMapper;
    
    /**
     * 执行文本翻译
     * 
     * @param request 翻译请求
     * @return 翻译结果
     */
    @PostMapping("/translate")
    public TranslationResponse translate(@Valid @RequestBody TranslationRequest request) throws Exception {
        log.info("收到翻译请求: {} -> {}", request.getSourceLanguage(), request.getTargetLanguage());
        
        // 统一从用户上下文填充用户ID（若上下文存在）
        if (request.getUserId() == null) {
            Long ctxUserId = UserContext.getUserId();
            if (ctxUserId != null) {
                request.setUserId(ctxUserId);
                log.info("已从用户上下文填充用户ID: {}", ctxUserId);
            } else {
                log.warn("用户上下文为空，未能填充用户ID");
            }
        }
        
        return translationService.translate(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter translateStream(@Valid @RequestBody TranslationRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);

        emitter.onError(ex -> log.warn("翻译SSE连接错误: {}", ex.getMessage()));
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("timeout"));
                emitter.complete();
            } catch (Exception ignore) {}
        });

        CompletableFuture.runAsync(() -> {
            try {
                if (request.getUserId() == null) {
                    Long ctxUserId = UserContext.getUserId();
                    if (ctxUserId != null) {
                        request.setUserId(ctxUserId);
                    }
                }

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
                        .data("{\"status\":\"翻译中...\"}"));

                int chunkSize = request.getSourceLanguage() != null && request.getSourceLanguage().startsWith("en") ? 8 : 16;
                long delayMs = request.getSourceLanguage() != null && request.getSourceLanguage().startsWith("en") ? 80L : 40L;

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
    
    /**
     * 批量翻译
     * 
     * @param requests 翻译请求列表
     * @return 翻译结果列表
     */
    @PostMapping("/batch-translate")
    public List<TranslationResponse> batchTranslate(@Valid @RequestBody List<TranslationRequest> requests) throws Exception {
        log.info("收到批量翻译请求，数量: {}", requests.size());
        
        // 统一从用户上下文填充用户ID（若上下文存在）
        Long ctxUserId = UserContext.getUserId();
        if (ctxUserId != null) {
            for (TranslationRequest req : requests) {
                if (req.getUserId() == null) {
                    req.setUserId(ctxUserId);
                }
            }
        } else {
            log.warn("批量翻译时用户上下文为空，未填充用户ID");
        }
        
        return translationService.batchTranslate(requests);
    }
    
    /**
     * 语言检测
     * 
     * @param text 待检测文本
     * @return 检测结果
     */
    @PostMapping("/detect-language")
    public LanguageDetectionResponse detectLanguage(
            @RequestBody @NotBlank(message = "检测文本不能为空") 
            @Size(max = 1000, message = "检测文本长度不能超过1000字符") String text) throws Exception {
        log.info("收到语言检测请求，文本长度: {}", text.length());
        return translationService.detectLanguage(text);
    }
    
    /**
     * 获取支持的语言列表
     * 
     * @return 支持的语言列表
     */
    @GetMapping("/supported-languages")
    public List<SupportedLanguageResponse> getSupportedLanguages() throws Exception {
        log.info("获取支持的语言列表");
        return translationService.getSupportedLanguages();
    }
    
    /**
     * 获取可用的翻译引擎列表
     * 
     * @return 翻译引擎列表
     */
    @GetMapping("/engines")
    public List<String> getAvailableEngines() throws Exception {
        log.info("获取可用翻译引擎列表");
        return translationService.getAvailableEngines();
    }
    
    /**
     * 检查语言对是否支持
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 是否支持
     */
    @GetMapping("/check-language-pair")
    public LanguagePairSupportResponse checkLanguagePair(
            @RequestParam @NotBlank(message = "源语言不能为空") String sourceLanguage,
            @RequestParam @NotBlank(message = "目标语言不能为空") String targetLanguage) throws Exception {
        log.info("检查语言对支持: {} -> {}", sourceLanguage, targetLanguage);
        boolean supported = translationService.isLanguagePairSupported(sourceLanguage, targetLanguage);
        return LanguagePairSupportResponse.builder()
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .supported(supported)
                .build();
    }

    /**
     * 依据术语库与风格 Prompt 对 MT 结果二次润色
     */
    @PostMapping("/polish")
    public PolishResponse polish(@Valid @RequestBody PolishRequest request) throws Exception {
        log.info("收到后编辑请求: targetLanguage={}, glossary filters: category={}, domain={}",
                request.getTargetLanguage(), request.getCategory(), request.getDomain());
        return polishService.polish(request);
    }
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.builder()
                .timestamp(System.currentTimeMillis())
                .service("Translation Service")
                .version("1.0.0")
                .build();
    }
}
