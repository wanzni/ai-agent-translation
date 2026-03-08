package cn.net.susan.ai.translation.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.enums.DocumentTypeEnum;
import cn.net.susan.ai.translation.service.TranslationService;
import cn.net.susan.ai.translation.service.file.PptxFileTranslator;
import org.apache.poi.xslf.usermodel.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 针对 PptxFileTranslator 的基础行为测试，验证：
 * - 翻译后文本被替换；
 * - 启用自动换行；
 * - 段落行距被设置为 120%；
 * - 文本框根据内容扩展（无法稳定断言数值，仅验证无异常）。
 */
public class PptxFileTranslatorTest {

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

        // 以下方法在本测试中不使用，返回空或默认值
        @Override public cn.net.susan.ai.translation.dto.LanguageDetectionResponse detectLanguage(String text) { return null; }
        @Override public List<cn.net.susan.ai.translation.dto.SupportedLanguageResponse> getSupportedLanguages() { return List.of(); }
        @Override public org.springframework.data.domain.Page<cn.net.susan.ai.translation.entity.TranslationRecord> getTranslationHistory(Long userId, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public cn.net.susan.ai.translation.entity.TranslationRecord getTranslationById(Long id) { return null; }
        @Override public boolean deleteTranslation(Long id) { return false; }
        @Override public cn.net.susan.ai.translation.dto.TranslationStatisticsResponse getTranslationStatistics(Long userId) { return new cn.net.susan.ai.translation.dto.TranslationStatisticsResponse(); }
        @Override public TranslationResponse retranslate(Long id, String engine) { return TranslationResponse.builder().translatedText("[TEST] RETRANSLATE").build(); }
        @Override public org.springframework.data.domain.Page<cn.net.susan.ai.translation.entity.TranslationRecord> searchTranslations(String keyword, Long userId, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public java.util.Map<String, Object> getQualityAssessment(Long translationId) { return java.util.Map.of(); }
        @Override public List<String> getAvailableEngines() { return List.of("MOCK"); }
        @Override public boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage) { return true; }
        @Override public String getTranslationCache(String sourceText, String sourceLanguage, String targetLanguage) { return null; }
        @Override public void setTranslationCache(String sourceText, String sourceLanguage, String targetLanguage, String translatedText) { }
    }

    @Test
    public void translate_shouldEnableWordWrapAndLineSpacing() throws Exception {
        // 1) 构造一个简单的 PPTX，包含一个文本框和长文本
        XMLSlideShow ppt = new XMLSlideShow();
        XSLFSlide slide = ppt.createSlide();

        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new java.awt.Rectangle(50, 50, 300, 80)); // 宽度较窄以触发换行

        XSLFTextParagraph p = box.addNewTextParagraph();
        XSLFTextRun r = p.addNewTextRun();
        r.setText("This is a long English sentence that will become longer after translation to simulate wrapping.");

        ByteArrayOutputStream srcOut = new ByteArrayOutputStream();
        ppt.write(srcOut);
        byte[] srcBytes = srcOut.toByteArray();

        // 2) 调用翻译器
        PptxFileTranslator translator = new PptxFileTranslator();
        DocumentTranslation task = DocumentTranslation.builder()
                .originalFilename("demo.pptx")
                .fileType(DocumentTypeEnum.PPTX)
                .sourceFilePath("/tmp/demo.pptx")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .translationEngine("MOCK")
                .build();

        byte[] outBytes = translator.translate(srcBytes, task, new StubTranslationService(), (percent, msg) -> {});

        // 3) 验证输出文件中的文本框属性
        try (XMLSlideShow out = new XMLSlideShow(new ByteArrayInputStream(outBytes))) {
            XSLFSlide outSlide = out.getSlides().get(0);
            XSLFTextShape outShape = null;
            for (XSLFShape s : outSlide.getShapes()) {
                if (s instanceof XSLFTextShape ts) {
                    outShape = ts; break;
                }
            }
            Assertions.assertNotNull(outShape, "输出中应存在文本框");

            // 自动换行应开启
            Assertions.assertTrue(outShape.getWordWrap(), "应开启自动换行以避免文本溢出");

            // 行距应设置为 120%
            boolean anyParagraph120 = false;
            for (XSLFTextParagraph para : outShape.getTextParagraphs()) {
                Double ls = para.getLineSpacing();
                if (ls != null && Math.abs(ls - 120.0) < 0.001) { anyParagraph120 = true; break; }
            }
            Assertions.assertTrue(anyParagraph120, "至少一个段落的行距应设置为120%");

            // 文本内容应被替换为以 [TEST] 开头
            String allText = outShape.getText();
            Assertions.assertTrue(allText.contains("[TEST]"), "文本应包含测试翻译标识");
        }
    }
}