package cn.net.wanzni.ai.translation.service.translation;

import cn.net.wanzni.ai.translation.config.DashscopeProperties;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QwenTranslationServiceTest {

    @Test
    void shouldKeepOriginalSourceTextInsteadOfPreprocessedGlossaryText() throws Exception {
        QwenTranslationService service = new QwenTranslationService(new DashscopeProperties());
        TranslationRequest request = TranslationRequest.builder()
                .sourceText("???? XL ????? L ?????")
                .sourceLanguage("zh")
                .targetLanguage("en")
                .useTerminology(true)
                .ragContext(Map.of(
                        "preprocessedSourceText", "[[sun protection]][[sun protection]]XL[[sun protection]]",
                        "glossaryMap", Map.of("??", "out of stock", "????", "full refund"),
                        "contextSnippets", List.of("SOURCE_TYPE: TM\nSRC: ??\nTGT: sample")
                ))
                .build();

        Method method = QwenTranslationService.class.getDeclaredMethod("buildTranslationPrompt", TranslationRequest.class);
        method.setAccessible(true);
        String prompt = (String) method.invoke(service, request);

        assertTrue(prompt.contains("???? XL ????? L ?????"));
        assertTrue(prompt.contains("?? => out of stock"));
        assertFalse(prompt.contains("[[sun protection]]"));
    }
}
