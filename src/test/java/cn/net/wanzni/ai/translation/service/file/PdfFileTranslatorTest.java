package cn.net.wanzni.ai.translation.service.file;

import cn.net.wanzni.ai.translation.dto.*;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.service.TranslationService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 基本自测：验证 PDFTextStripper 提取 + 叠加译文输出无乱码，数字不翻译。
 */
public class PdfFileTranslatorTest {

    @Test
    public void testOverlayTranslation_NoGarbledAndSkipNumbers() throws Exception {
        // 构造简单 PDF
        byte[] sourcePdf;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 14);
                cs.newLineAtOffset(50, 700);
                cs.showText("Hello, Susan AI");
                cs.newLineAtOffset(0, -18);
                cs.showText("2024-11-10"); // 数字样式，应跳过
                cs.endText();
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                sourcePdf = baos.toByteArray();
            }
        }

        // 构造任务
        DocumentTranslation task = DocumentTranslation.builder()
                .sourceLanguage("auto")
                .targetLanguage("en")
                .translationEngine("mock")
                .build();

        // 构造翻译服务（简单回显）
        TranslationService mockService = new TranslationService() {
            @Override
            public TranslationResponse translate(TranslationRequest request) {
                return TranslationResponse.builder().translatedText("TRANSLATED TEXT").build();
            }

            @Override
            public List<TranslationResponse> batchTranslate(List<TranslationRequest> requests) {
                List<TranslationResponse> list = new ArrayList<>();
                for (TranslationRequest r : requests) {
                    list.add(TranslationResponse.builder().translatedText("TRANSLATED TEXT").build());
                }
                return list;
            }

            @Override
            public List<TranslationResponse> parallelBatchTranslate(List<TranslationRequest> requests) {
                return batchTranslate(requests);
            }

            @Override
            public LanguageDetectionResponse detectLanguage(String text) {
                return LanguageDetectionResponse.builder()
                        .language("en").confidence(0.99).success(true).build();
            }

            @Override
            public List<SupportedLanguageResponse> getSupportedLanguages() { return List.of(); }

            @Override
            public org.springframework.data.domain.Page<TranslationRecord> getTranslationHistory(Long userId, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }

            @Override
            public TranslationRecord getTranslationById(Long id) { return null; }

            @Override
            public boolean deleteTranslation(Long id) { return true; }

            @Override
            public Map<String, Object> getQualityAssessment(Long translationId) { return Map.of(); }

            @Override
            public TranslationStatisticsResponse getTranslationStatistics(Long userId) {
                return TranslationStatisticsResponse.builder().totalTranslations(0L).build();
            }

            @Override
            public TranslationResponse retranslate(Long id, String engine) {
                return cn.net.wanzni.ai.translation.dto.TranslationResponse.builder().translatedText("RETRANSLATED").build();
            }

            @Override
            public List<String> getAvailableEngines() { return List.of("MOCK"); }

            @Override
            public boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage) { return true; }

            @Override
            public String getTranslationCache(String sourceText, String sourceLanguage, String targetLanguage) { return null; }

            @Override
            public void setTranslationCache(String sourceText, String sourceLanguage, String targetLanguage, String translatedText) {}

            @Override
            public org.springframework.data.domain.Page<TranslationRecord> searchTranslations(String keyword, Long userId, org.springframework.data.domain.Pageable pageable) {
                return org.springframework.data.domain.Page.empty();
            }
        };

        // 进度回调
        BiConsumer<Integer, String> progress = (p, m) -> {};

        // 调用翻译器
        PdfFileTranslator translator = new PdfFileTranslator();
        byte[] translatedBytes = translator.translate(sourcePdf, task, mockService, progress);

        // 提取译文验证（应出现叠加的英文，且数字行仍为原样）
        try (PDDocument outDoc = Loader.loadPDF(translatedBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String outText = stripper.getText(outDoc);

            // 新PDF应包含译文，不应保留原文
            Assertions.assertTrue(outText.contains("TRANSLATED TEXT"), "新PDF应包含英文翻译");
            Assertions.assertFalse(outText.contains("Hello, Susan AI"), "新PDF不应保留原始文本");
            // 数字样式保留
            Assertions.assertTrue(outText.contains("2024-11-10"), "数字样式应保留不翻译");
            // 页数对应
            Assertions.assertEquals(1, outDoc.getNumberOfPages(), "新PDF页数应与原PDF一致");
        }
    }
}