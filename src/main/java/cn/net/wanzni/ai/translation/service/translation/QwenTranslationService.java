package cn.net.wanzni.ai.translation.service.translation;

import cn.net.wanzni.ai.translation.config.DashscopeProperties;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.dto.LanguageDetectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 Spring WebClient 对接阿里 DashScope（通义千问）Chat Completions，实现文本翻译。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenTranslationService implements ThirdPartyTranslator {

    private final DashscopeProperties properties;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 执行文本翻译
     */
    @Override
    public TranslationResponse translate(TranslationRequest request) throws Exception {
        long start = System.currentTimeMillis();

        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key 未配置，请在 ai.dashscope.api-key 中设置");
        }

        String prompt = buildTranslationPrompt(request);

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional translator. Follow instructions strictly."),
                Map.of("role", "user", "content", prompt)
        ));
        if (properties.getTemperature() != null) {
            body.put("temperature", properties.getTemperature());
        }

        String url = properties.getBaseUrl();

        try {
            if (log.isDebugEnabled()) {
                log.debug("DashScope 请求URL: {}", url);
                log.debug("DashScope 请求模型: {}", body.get("model"));
                log.debug("DashScope 请求体: {}", body);
            }

            Map<String, Object> resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 4xx 响应: status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 4xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 5xx 响应: status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 5xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .bodyToMono(Map.class)
                    .onErrorResume(err -> {
                        log.error("DashScope 调用异常: {}", err.getMessage(), err);
                        return Mono.error(err);
                    })
                    .block();

            String translated = extractTranslatedText(resp);
            long cost = System.currentTimeMillis() - start;

            if (!StringUtils.hasText(translated)) {
                String errMsg = String.valueOf(resp);
                return TranslationResponse.builder()
                        .sourceText(request.getSourceText())
                        .sourceLanguage(request.getSourceLanguage())
                        .targetLanguage(request.getTargetLanguage())
                        .translationEngine("QWEN")
                        .status("ERROR")
                        .errorMessage(errMsg)
                        .characterCount(request.getSourceText().length())
                        .processingTime(cost)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            return TranslationResponse.builder()
                    .translatedText(translated)
                    .sourceText(request.getSourceText())
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .translationEngine("QWEN")
                    .processingTime(cost)
                    .status("COMPLETED")
                    .characterCount(request.getSourceText().length())
                    .qualityScore(90.0)
                    .confidence(0.9)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("调用 Qwen 翻译失败: {}", e.getMessage(), e);
            return TranslationResponse.builder()
                    .sourceText(request.getSourceText())
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .translationEngine("QWEN")
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .characterCount(request.getSourceText().length())
                    .processingTime(cost)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 使用 DashScope Chat Completions 进行语言检测
     */
    @SuppressWarnings("unchecked")
    @Override
    public LanguageDetectionResponse detectLanguage(String text) throws Exception {
        long start = System.currentTimeMillis();

        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key 未配置，请在 ai.dashscope.api-key 中设置");
        }

        String url = properties.getBaseUrl();

        String systemPrompt = "You are a language detection assistant. Respond ONLY with ISO 639-1 code (e.g., zh, en, ja, ko, fr, de, es, ru, ar, pt). If multilingual, choose the dominant.";
        String userPrompt = "Detect the language code for this text:\n\n" + text;

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", 0.0);

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
                                log.error("DashScope 4xx 响应(检测): status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 4xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 5xx 响应(检测): status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 5xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .bodyToMono(Map.class)
                    .block();

            String code = extractTranslatedText(resp);
            String normalized = normalizeLanguageCode(code);

            long cost = System.currentTimeMillis() - start;
            return LanguageDetectionResponse.builder()
                    .language(normalized)
                    .confidence(0.9)
                    .success(StringUtils.hasText(normalized))
                    .processingTime(cost)
                    .build();

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("Qwen 语言检测失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 解析 DashScope 响应中的文本：output.choices[0].message.content
     */
    @SuppressWarnings("unchecked")
    private String extractTranslatedText(Map<String, Object> resp) {
        if (resp == null) return null;
        // 兼容两种响应结构：
        // 1) 旧版：output.choices[0].message.content
        // 2) OpenAI兼容模式：choices[0].message.content
        Object choicesObj = null;
        Object output = resp.get("output");
        if (output instanceof Map<?, ?> out) {
            choicesObj = out.get("choices");
        }
        if (choicesObj == null) {
            choicesObj = resp.get("choices");
        }
        if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> m) {
                Object message = m.get("message");
                if (message instanceof Map<?, ?> mm) {
                    Object content = mm.get("content");
                    if (content instanceof String cs && StringUtils.hasText(cs)) {
                        return cs.trim();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 构造翻译提示：只输出译文，不要解释。
     */
    private String buildTranslationPrompt(TranslationRequest request) {
        String sourceText = request.getSourceText();
        String sourceLang = request.getSourceLanguage();
        String targetLang = request.getTargetLanguage();
        String src = (sourceLang == null || sourceLang.equalsIgnoreCase("auto")) ? "auto" : sourceLang;

        StringBuilder sb = new StringBuilder();
        // 系统指令：严格翻译与术语约束
        sb.append("You are a professional translator. Follow instructions strictly.\n");
        sb.append("- Task: Translate from ").append(src).append(" to ").append(targetLang).append(".\n");
        sb.append("- Output only the translated text, no explanations.\n");
        if (Boolean.TRUE.equals(request.getUseTerminology())) {
            sb.append("- Enforce terminology constraints if provided.\n");
            sb.append("- If the source contains tokens like [[...]], those are enforced glossary translations. Keep the text inside unchanged and remove the brackets in the final output.\n");
        }

        // 术语与历史上下文（RAG）
        Map<String, Object> rag = request.getRagContext();
        if (rag != null) {
            // 使用RAG生成的预处理源文本（前置术语约束）
            Object preObj = rag.get("preprocessedSourceText");
            if (preObj instanceof String ps && org.springframework.util.StringUtils.hasText(ps)) {
                sourceText = ps;
            }
            Object glossaryObj = rag.get("glossaryMap");
            if (glossaryObj instanceof Map<?,?> gm && !((Map<?,?>) gm).isEmpty()) {
                sb.append("\n[Terminology constraints]\n");
                @SuppressWarnings("unchecked")
                Map<String, String> glossaryMap = (Map<String, String>) gm;
                for (Map.Entry<String, String> e : glossaryMap.entrySet()) {
                    sb.append(e.getKey()).append(" => ").append(e.getValue()).append("\n");
                }
            }
            Object ctxSnippetsObj = rag.get("contextSnippets");
            if (ctxSnippetsObj instanceof List<?> lst && !lst.isEmpty()) {
                sb.append("\n[Relevant context]\n");
                for (Object o : lst) {
                    if (o != null) sb.append(String.valueOf(o)).append("\n");
                }
            }
        }

        sb.append("\n[Text]\n").append(sourceText);
        return sb.toString();
    }

    /**
     * 语言代码规范化（支持 zh-CN/en-US 等，或中文名称映射）
     */
    private String normalizeLanguageCode(String code) {
        if (!StringUtils.hasText(code)) return null;
        String s = code.trim().toLowerCase();
        // 去除可能的无关字符
        s = s.replaceAll("[^a-z-]", "");
        if (s.startsWith("zh")) return "zh";
        if (s.startsWith("en")) return "en";
        if (s.startsWith("ja")) return "ja";
        if (s.startsWith("ko")) return "ko";
        if (s.startsWith("fr")) return "fr";
        if (s.startsWith("de")) return "de";
        if (s.startsWith("es")) return "es";
        if (s.startsWith("ru")) return "ru";
        if (s.startsWith("pt")) return "pt";
        if (s.startsWith("ar")) return "ar";
        // 中文名称兜底
        switch (s) {
            case "中文": return "zh";
            case "英语": return "en";
            case "日语": return "ja";
            case "韩语": return "ko";
            case "法语": return "fr";
            case "德语": return "de";
            case "西班牙语": return "es";
            case "俄语": return "ru";
            case "葡萄牙语": return "pt";
            case "阿拉伯语": return "ar";
            default: return s.length() == 2 ? s : null;
        }
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key is not configured");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (properties.getTemperature() != null) {
            body.put("temperature", properties.getTemperature());
        }

        Map<String, Object> resp = webClient.post()
                .uri(properties.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (ClientResponse r) ->
                        r.bodyToMono(String.class).flatMap(errBody ->
                                Mono.error(new RuntimeException("DashScope 4xx: " + r.statusCode() + " - " + errBody))))
                .onStatus(HttpStatusCode::is5xxServerError, (ClientResponse r) ->
                        r.bodyToMono(String.class).flatMap(errBody ->
                                Mono.error(new RuntimeException("DashScope 5xx: " + r.statusCode() + " - " + errBody))))
                .bodyToMono(Map.class)
                .block();
        return extractTranslatedText(resp);
    }

    @Override
    public String getEngineCode() {
        return "QWEN";
    }
}
