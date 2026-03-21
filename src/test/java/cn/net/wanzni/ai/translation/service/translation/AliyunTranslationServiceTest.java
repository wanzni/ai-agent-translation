package cn.net.wanzni.ai.translation.service.translation;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阿里云机器翻译服务测试
 */
@SpringBootTest
public class AliyunTranslationServiceTest {

    @Autowired
    private AliyunTranslationService aliyunTranslationService;

    @Test
    public void testTranslateChineseToEnglish() throws Exception {
        System.out.println("========== Testing Machine Translation: English -> Chinese ==========");

        TranslationRequest request = TranslationRequest.builder()
                .sourceText("Hello World")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build();

        TranslationResponse response = aliyunTranslationService.translate(request);

        System.out.println("Source Text: " + response.getSourceText());
        System.out.println("Translated Text: " + response.getTranslatedText());
        System.out.println("Source Language: " + response.getSourceLanguage());
        System.out.println("Target Language: " + response.getTargetLanguage());
        System.out.println("Translation Engine: " + response.getTranslationEngine());
        System.out.println("Status: " + response.getStatus());
        System.out.println("Error Message: " + response.getErrorMessage());
        System.out.println("Processing Time: " + response.getProcessingTime() + "ms");

        // Print error details if failed
        if ("ERROR".equals(response.getStatus())) {
            System.out.println("ERROR: Translation failed with message: " + response.getErrorMessage());
        }

        assertNotNull(response.getTranslatedText(), "Translated text should not be null, error: " + response.getErrorMessage());
        assertEquals("COMPLETED", response.getStatus(), "Status should be COMPLETED");
        assertEquals("ALIBABA_CLOUD", response.getTranslationEngine(), "Engine should be ALIBABA_CLOUD");

        System.out.println("========== Test Completed ==========");
    }

    @Test
    public void testTranslateEnglishToChinese() throws Exception {
        System.out.println("========== 测试机器翻译：英文 → 中文 ==========");

        TranslationRequest request = TranslationRequest.builder()
                .sourceText("Hello, World")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build();

        TranslationResponse response = aliyunTranslationService.translate(request);

        System.out.println("源文本: " + response.getSourceText());
        System.out.println("翻译结果: " + response.getTranslatedText());
        System.out.println("源语言: " + response.getSourceLanguage());
        System.out.println("目标语言: " + response.getTargetLanguage());
        System.out.println("翻译引擎: " + response.getTranslationEngine());
        System.out.println("状态: " + response.getStatus());
        System.out.println("耗时: " + response.getProcessingTime() + "ms");

        assertNotNull(response.getTranslatedText(), "翻译结果不应为空");
        assertEquals("COMPLETED", response.getStatus(), "翻译状态应为完成");

        System.out.println("========== 测试完成 ==========");
    }

    @Test
    public void testTranslateLongText() throws Exception {
        System.out.println("========== 测试机器翻译：长文本 ==========");

        String longText = "机器学习是人工智能的一个分支，它使计算机能够从数据中学习并改进性能，而无需明确编程。";

        TranslationRequest request = TranslationRequest.builder()
                .sourceText(longText)
                .sourceLanguage("zh")
                .targetLanguage("en")
                .build();

        TranslationResponse response = aliyunTranslationService.translate(request);

        System.out.println("源文本: " + response.getSourceText());
        System.out.println("翻译结果: " + response.getTranslatedText());
        System.out.println("字符数: " + response.getCharacterCount());
        System.out.println("耗时: " + response.getProcessingTime() + "ms");

        assertNotNull(response.getTranslatedText(), "翻译结果不应为空");
        assertTrue(response.getTranslatedText().length() > 0, "翻译结果应有内容");

        System.out.println("========== 测试完成 ==========");
    }
}
