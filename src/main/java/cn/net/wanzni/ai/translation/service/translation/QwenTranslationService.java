package cn.net.wanzni.ai.translation.service.translation;

import cn.net.wanzni.ai.translation.config.DashscopeProperties;
import cn.net.wanzni.ai.translation.dto.LanguageDetectionResponse;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenTranslationService implements ThirdPartyTranslator {

    private final DashscopeProperties properties;
    private final WebClient webClient = WebClient.builder().build();

    @Override
    public TranslationResponse translate(TranslationRequest request) throws Exception {
        long start = System.currentTimeMillis();

        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key ?????? ai.dashscope.api-key ???");
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

        try {
            Map<String, Object> resp = webClient.post()
                    .uri(properties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::mapDashscopeError)
                    .onStatus(HttpStatusCode::is5xxServerError, this::mapDashscopeError)
                    .bodyToMono(Map.class)
                    .onErrorResume(err -> {
                        log.error("DashScope ????: {}", err.getMessage(), err);
                        return Mono.error(err);
                    })
                    .block();

            String translated = extractTranslatedText(resp);
            long cost = System.currentTimeMillis() - start;

            if (!StringUtils.hasText(translated)) {
                return TranslationResponse.builder()
                        .sourceText(request.getSourceText())
                        .sourceLanguage(request.getSourceLanguage())
                        .targetLanguage(request.getTargetLanguage())
                        .translationEngine("QWEN")
                        .status("ERROR")
                        .errorMessage(String.valueOf(resp))
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
            log.error("?? Qwen ????: {}", e.getMessage(), e);
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

    @SuppressWarnings("unchecked")
    @Override
    public LanguageDetectionResponse detectLanguage(String text) throws Exception {
        long start = System.currentTimeMillis();

        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key ?????? ai.dashscope.api-key ???");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are a language detection assistant. Respond ONLY with ISO 639-1 code (e.g., zh, en, ja, ko, fr, de, es, ru, ar, pt). If multilingual, choose the dominant."),
                Map.of("role", "user", "content", "Detect the language code for this text:\n\n" + text)
        ));
        body.put("temperature", 0.0);

        Map<String, Object> resp = webClient.post()
                .uri(properties.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::mapDashscopeError)
                .onStatus(HttpStatusCode::is5xxServerError, this::mapDashscopeError)
                .bodyToMono(Map.class)
                .block();

        String code = extractTranslatedText(resp);
        String normalized = normalizeLanguageCode(code);

        return LanguageDetectionResponse.builder()
                .language(normalized)
                .confidence(0.9)
                .success(StringUtils.hasText(normalized))
                .processingTime(System.currentTimeMillis() - start)
                .build();
    }

    private Mono<? extends Throwable> mapDashscopeError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(errBody -> Mono.error(new RuntimeException(
                        "DashScope error: " + response.statusCode() + " - " + errBody
                )));
    }

    @SuppressWarnings("unchecked")
    private String extractTranslatedText(Map<String, Object> resp) {
        if (resp == null) {
            return null;
        }
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

    private String buildTranslationPrompt(TranslationRequest request) {
        String sourceText = request.getSourceText();
        String sourceLang = request.getSourceLanguage();
        String targetLang = request.getTargetLanguage();
        String src = (sourceLang == null || sourceLang.equalsIgnoreCase("auto")) ? "auto" : sourceLang;

        StringBuilder sb = new StringBuilder();
        sb.append("You are a professional translator. Follow instructions strictly.\n");
        sb.append("- Task: Translate from ").append(src).append(" to ").append(targetLang).append(".\n");
        sb.append("- Output only the translated text, no explanations.\n");
        if (Boolean.TRUE.equals(request.getUseTerminology())) {
            sb.append("- Enforce terminology constraints if provided.\n");
        }

        Map<String, Object> rag = request.getRagContext();
        if (rag != null) {
            Object glossaryObj = rag.get("glossaryMap");
            if (glossaryObj instanceof Map<?, ?> gm && !((Map<?, ?>) gm).isEmpty()) {
                sb.append("\n[Terminology constraints]\n");
                @SuppressWarnings("unchecked")
                Map<String, String> glossaryMap = (Map<String, String>) gm;
                for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
                    sb.append(entry.getKey()).append(" => ").append(entry.getValue()).append("\n");
                }
            }
            Object ctxSnippetsObj = rag.get("contextSnippets");
            if (ctxSnippetsObj instanceof List<?> snippets && !snippets.isEmpty()) {
                sb.append("\n[Relevant context]\n");
                for (Object snippet : snippets) {
                    if (snippet != null) {
                        sb.append(String.valueOf(snippet)).append("\n");
                    }
                }
            }
        }

        sb.append("\n[Text]\n").append(sourceText);
        return sb.toString();
    }

    private String normalizeLanguageCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String s = code.trim().toLowerCase().replaceAll("[^a-z-]", "");
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
        return s.length() == 2 ? s : null;
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
                .onStatus(HttpStatusCode::is4xxClientError, this::mapDashscopeError)
                .onStatus(HttpStatusCode::is5xxServerError, this::mapDashscopeError)
                .bodyToMono(Map.class)
                .block();
        return extractTranslatedText(resp);
    }

    @Override
    public String getEngineCode() {
        return "QWEN";
    }
}
