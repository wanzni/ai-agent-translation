package cn.net.wanzni.ai.translation.file;

import cn.net.wanzni.ai.translation.dto.*;
import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.enums.DocumentTypeEnum;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.service.TranslationService;
import cn.net.wanzni.ai.translation.service.file.PptFileTranslator;
import org.apache.poi.hslf.usermodel.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 针对旧版 PPT（HSLF）的翻译器测试，验证：
 * - 翻译后文本被替换；
 * - 启用自动换行；
 * - 段落行距被设置为 120%。
 */
public class PptFileTranslatorTest {

    private static class StubTranslationService implements TranslationService {
        @Override
        public TranslationResponse translate(TranslationRequest request) {
            return TranslationResponse.builder()
                    .translatedText("[TEST] " + request.getSourceText())
                    .build();
        }

        @Override
        public List<TranslationResponse> batchTranslate(List<TranslationRequest> requests) {
            List<TranslationResponse> out = new ArrayList<>();
            for (TranslationRequest r : requests) {
                out.add(TranslationResponse.builder()
                        .translatedText("[TEST] " + r.getSourceText() + " [更多内容以触发换行]")
                        .build());
            }
            return out;
        }

        @Override
        public List<TranslationResponse> parallelBatchTranslate(List<TranslationRequest> requests) {
            return batchTranslate(requests);
        }

        @Override public LanguageDetectionResponse detectLanguage(String text) { return null; }
        @Override public List<SupportedLanguageResponse> getSupportedLanguages() { return List.of(); }
        @Override public org.springframework.data.domain.Page<TranslationRecord> getTranslationHistory(Long userId, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public TranslationRecord getTranslationById(Long id) { return null; }
        @Override public boolean deleteTranslation(Long id) { return false; }
        @Override public TranslationStatisticsResponse getTranslationStatistics(Long userId) { return new TranslationStatisticsResponse(); }
        @Override public TranslationResponse retranslate(Long id, String engine) { return TranslationResponse.builder().translatedText("[TEST] RETRANSLATE").build(); }
        @Override public org.springframework.data.domain.Page<TranslationRecord> searchTranslations(String keyword, Long userId, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public java.util.Map<String, Object> getQualityAssessment(Long translationId) { return java.util.Map.of(); }
        @Override public List<String> getAvailableEngines() { return List.of("MOCK"); }
        @Override public boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage) { return true; }
        @Override public String getTranslationCache(String sourceText, String sourceLanguage, String targetLanguage) { return null; }
        @Override public void setTranslationCache(String sourceText, String sourceLanguage, String targetLanguage, String translatedText) { }
    }

    @Test
    public void translate_shouldEnableWordWrapAndLineSpacing() throws Exception {
        // 1) 构造一个 HSLF PPT，包含一个文本框和长文本
        HSLFSlideShow ppt = new HSLFSlideShow();
        HSLFSlide slide = ppt.createSlide();

        HSLFTextBox box = new HSLFTextBox();
        box.setAnchor(new java.awt.Rectangle(60, 60, 300, 80));
        box.setText("This is a long English sentence which will be translated to simulate wrapping in legacy PPT.");
        slide.addShape(box);

        ByteArrayOutputStream srcOut = new ByteArrayOutputStream();
        ppt.write(srcOut);
        byte[] srcBytes = srcOut.toByteArray();

        // 2) 调用翻译器
        PptFileTranslator translator = new PptFileTranslator();
        DocumentTranslation task = DocumentTranslation.builder()
                .originalFilename("demo.ppt")
                .fileType(DocumentTypeEnum.PPT)
                .sourceFilePath("/tmp/demo.ppt")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .translationEngine("MOCK")
                .build();

        byte[] outBytes = translator.translate(srcBytes, task, new StubTranslationService(), (percent, msg) -> {});

        // 3) 验证输出文件中的文本框属性
        try (HSLFSlideShow out = new HSLFSlideShow(new ByteArrayInputStream(outBytes))) {
            HSLFSlide outSlide = out.getSlides().get(0);
            HSLFTextShape outShape = null;
            for (HSLFShape s : outSlide.getShapes()) {
                if (s instanceof HSLFTextShape ts) { outShape = ts; break; }
            }
            Assertions.assertNotNull(outShape, "输出中应存在文本框");

            Assertions.assertTrue(outShape.getWordWrap(), "应开启自动换行以避免文本溢出");

            boolean anyParagraph120 = false;
            List<HSLFTextParagraph> paragraphs = outShape.getTextParagraphs();
            if (paragraphs != null) {
                for (HSLFTextParagraph para : paragraphs) {
                    Double ls = para.getLineSpacing();
                    if (ls != null && Math.abs(ls - 120.0) < 0.001) { anyParagraph120 = true; break; }
                }
            }
            Assertions.assertTrue(anyParagraph120, "至少一个段落的行距应设置为120%");

            String allText = outShape.getText();
            Assertions.assertTrue(allText.contains("[TEST]"), "文本应包含测试翻译标识");
        }
    }
}