package cn.net.wanzni.ai.translation.core.agent.tools;

import cn.net.wanzni.ai.translation.core.agent.Tool;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationTool implements Tool {

    private final TranslationService translationService;

    @Override
    public String getName() {
        return "translate";
    }

    @Override
    public String getDescription() {
        return "Execute the actual translation. Returns the translated text.";
    }

    @Override
    public Map<String, ToolParam> getParams() {
        Map<String, ToolParam> params = new HashMap<>();
        params.put("text", new ToolParam("text", "string", "Source text to translate", true));
        params.put("sourceLang", new ToolParam("sourceLang", "string", "Source language code", true));
        params.put("targetLang", new ToolParam("targetLang", "string", "Target language code", true));
        params.put("domain", new ToolParam("domain", "string", "Domain field", false));
        return params;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) throws Exception {
        String text = (String) params.get("text");
        String sourceLang = (String) params.get("sourceLang");
        String targetLang = (String) params.get("targetLang");
        String domain = (String) params.get("domain");

        if (text == null || text.isBlank()) {
            return ToolResult.fail("Text parameter is required");
        }
        if (sourceLang == null || sourceLang.isBlank()) {
            return ToolResult.fail("sourceLang parameter is required");
        }
        if (targetLang == null || targetLang.isBlank()) {
            return ToolResult.fail("targetLang parameter is required");
        }

        TranslationRequest request = TranslationRequest.builder()
                .sourceText(text)
                .sourceLanguage(sourceLang)
                .targetLanguage(targetLang)
                .domain(domain)
                .useTerminology(true)
                .useRag(true)
                .build();

        TranslationResponse response = translationService.translate(request);
        return ToolResult.ok(response.getTranslatedText());
    }
}