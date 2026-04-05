package cn.net.wanzni.ai.translation.service.file;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * PDF translator that extracts text line-by-line, translates in batch, and
 * overlays translated text onto a newly generated PDF.
 */
@Slf4j
public class PdfFileTranslator implements DocumentFileTranslator {

    @Override
    public byte[] translate(byte[] sourceBytes,
                            DocumentTranslation task,
                            TranslationService translationService,
                            BiConsumer<Integer, String> progressCallback) throws Exception {
        try (PDDocument document = Loader.loadPDF(sourceBytes)) {
            int pageCount = document.getNumberOfPages();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<String> allLines = new ArrayList<>();
            List<Integer> lineToPage = new ArrayList<>();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                stripper.setStartPage(pageIndex + 1);
                stripper.setEndPage(pageIndex + 1);
                String pageText = stripper.getText(document);
                if (pageText == null) {
                    pageText = "";
                }

                int added = 0;
                for (String line : pageText.split("\r?\n")) {
                    if (line == null) {
                        continue;
                    }
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    allLines.add(trimmed);
                    lineToPage.add(pageIndex);
                    added++;
                }

                if (added > 0 && (pageIndex + 1) % 5 == 0) {
                    progressCallback.accept(Math.min(40, 10 + pageIndex), "已提取文本，页 " + (pageIndex + 1));
                }
            }

            if (allLines.isEmpty()) {
                progressCallback.accept(60, "未提取到可翻译文本，原样输出");
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    document.save(baos);
                    return baos.toByteArray();
                }
            }

            List<TranslationRequest> requests = new ArrayList<>();
            List<Boolean> translateMask = new ArrayList<>();
            for (String line : allLines) {
                boolean skip = isNumberLike(line);
                translateMask.add(!skip);
                if (!skip) {
                    requests.add(TranslationRequest.builder()
                            .sourceText(line)
                            .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                            .targetLanguage(task.getTargetLanguage())
                            .translationType("TEXT")
                            .translationEngine(task.getTranslationEngine())
                            .build());
                }
            }

            progressCallback.accept(55, "并行翻译中，共 " + requests.size() + " 段");
            List<TranslationResponse> responses = translationService.parallelBatchTranslate(requests);

            List<String> translatedLines = new ArrayList<>(allLines.size());
            int responseIndex = 0;
            for (int i = 0; i < allLines.size(); i++) {
                String original = allLines.get(i);
                if (!translateMask.get(i)) {
                    translatedLines.add(original);
                    continue;
                }

                String translated = null;
                if (responses != null && responseIndex < responses.size() && responses.get(responseIndex) != null) {
                    translated = responses.get(responseIndex).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    try {
                        TranslationResponse retry = translationService.translate(TranslationRequest.builder()
                                .sourceText(original)
                                .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                                .targetLanguage(task.getTargetLanguage())
                                .translationType("TEXT")
                                .translationEngine(task.getTranslationEngine())
                                .build());
                        if (retry != null && retry.getTranslatedText() != null && !retry.getTranslatedText().isBlank()) {
                            translated = retry.getTranslatedText();
                        }
                    } catch (Exception ignore) {
                    }
                }
                translatedLines.add((translated == null || translated.isBlank()) ? original : translated);
                responseIndex++;
            }

            try (PDDocument newDoc = new PDDocument()) {
                for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                    PDPage sourcePage = document.getPage(pageIndex);
                    PDRectangle mediaBox = sourcePage.getMediaBox();
                    PDPage newPage = new PDPage(mediaBox);
                    newDoc.addPage(newPage);

                    copyImagesToNewPage(sourcePage, newPage, newDoc);

                    float margin = 36f;
                    float fontSize = 12f;
                    float leading = 1.4f * fontSize;
                    float x = margin;
                    float yStart = mediaBox.getHeight() - margin;

                    try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage)) {
                        cs.setNonStrokingColor(Color.WHITE);
                        cs.addRect(0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                        cs.fill();

                        cs.setNonStrokingColor(Color.BLACK);
                        cs.beginText();
                        cs.setFont(new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), fontSize);
                        cs.newLineAtOffset(x, yStart);

                        float used = 0f;
                        for (int i = 0; i < translatedLines.size(); i++) {
                            if (lineToPage.get(i) != pageIndex) {
                                continue;
                            }
                            String line = translatedLines.get(i);
                            int maxCols = 80;
                            int offset = 0;
                            while (offset < line.length()) {
                                int end = Math.min(offset + maxCols, line.length());
                                cs.showText(line.substring(offset, end));
                                cs.newLineAtOffset(0, -leading);
                                used += leading;
                                if (used > (yStart - margin)) {
                                    break;
                                }
                                offset = end;
                            }
                            if (used > (yStart - margin)) {
                                break;
                            }
                        }

                        cs.endText();
                    }
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    newDoc.save(baos);
                    progressCallback.accept(90, "PDF 生成完成，准备上传");
                    return baos.toByteArray();
                }
            }
        }
    }

    private static boolean isNumberLike(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return trimmed.matches("[\\s0-9.,:;\\-+()/%]+");
    }

    private static void copyImagesToNewPage(PDPage sourcePage, PDPage newPage, PDDocument newDoc) {
        try {
            if (sourcePage.getResources() == null) {
                return;
            }
            log.debug("PDF page contains image resources; current translator keeps text path simple");
        } catch (Exception e) {
            log.warn("Failed to inspect PDF page resources: {}", e.getMessage());
        }
    }
}
