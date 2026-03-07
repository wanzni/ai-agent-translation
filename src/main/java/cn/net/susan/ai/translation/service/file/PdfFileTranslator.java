package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.awt.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * PDF 翻译器：解析内容流中的 Tj/TJ 文本操作符，批量翻译并回写字符串，保持字体与大小不变。
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

            // 使用 PDFTextStripper 可靠提取文本（基于 ToUnicode 映射），避免乱码
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<String> allLines = new ArrayList<>();
            List<Integer> lineToPage = new ArrayList<>();

            for (int pi = 0; pi < pageCount; pi++) {
                stripper.setStartPage(pi + 1);
                stripper.setEndPage(pi + 1);
                String pageText = stripper.getText(document);
                if (pageText == null) pageText = "";
                String[] lines = pageText.split("\r?\n");
                int added = 0;
                for (String line : lines) {
                    if (line == null) continue;
                    String t = line.trim();
                    if (t.isEmpty()) continue;
                    allLines.add(t);
                    lineToPage.add(pi);
                    added++;
                }
                if (added > 0 && (pi + 1) % 5 == 0) {
                    progressCallback.accept(Math.min(40, 10 + pi), "已提取文本，页:" + (pi + 1));
                }
            }

            if (allLines.isEmpty()) {
                progressCallback.accept(60, "未提取到有效文本，原样输出");
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    document.save(baos);
                    return baos.toByteArray();
                }
            }

            // 构建批量请求（跳过纯数字/数字样式），并保留映射
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

            progressCallback.accept(55, "批量翻译中，共" + requests.size() + "段");
            List<TranslationResponse> responses = translationService.batchTranslate(requests);

            List<String> translatedLines = new ArrayList<>(allLines.size());
            int ri = 0;
            for (int i = 0; i < allLines.size(); i++) {
                String original = allLines.get(i);
                String translated;
                if (!translateMask.get(i)) {
                    translated = original; // 数字样式保留
                } else {
                    translated = null;
                    if (responses != null && ri < responses.size() && responses.get(ri) != null) {
                        translated = responses.get(ri).getTranslatedText();
                    }
                    if (translated == null || translated.isBlank()) {
                        try {
                            TranslationRequest req = TranslationRequest.builder()
                                    .sourceText(original)
                                    .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                                    .targetLanguage(task.getTargetLanguage())
                                    .translationType("TEXT")
                                    .translationEngine(task.getTranslationEngine())
                                    .build();
                            TranslationResponse retry = translationService.translate(req);
                            if (retry != null && retry.getTranslatedText() != null && !retry.getTranslatedText().isBlank()) {
                                translated = retry.getTranslatedText();
                            }
                        } catch (Exception ignore) {}
                    }
                    if (translated == null || translated.isBlank()) {
                        translated = original; // 仍为空保留原文，避免乱码
                    }
                    ri++;
                }
                translatedLines.add(translated);
            }

            // 生成全新的 PDF：每个原页面对应一个新页面，按从上到下的顺序写入译文
            try (PDDocument newDoc = new PDDocument()) {
                for (int pi = 0; pi < pageCount; pi++) {
                    PDPage srcPage = document.getPage(pi);
                    PDRectangle mediaBox = srcPage.getMediaBox();
                    PDPage newPage = new PDPage(mediaBox);
                    newDoc.addPage(newPage);

                    try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        cs.setNonStrokingColor(Color.WHITE);
                        cs.addRect(0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                        cs.fill();
                    }

                    float margin = 36f;
                    float fontSize = 12f;
                    float leading = 1.4f * fontSize;
                    float x = margin;
                    float yStart = mediaBox.getHeight() - margin; // 从页顶开始

                    try (PDPageContentStream cs = new PDPageContentStream(newDoc, newPage)) {
                        cs.setNonStrokingColor(Color.WHITE);
                        cs.addRect(0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                        cs.fill();

                        cs.setNonStrokingColor(Color.BLACK);
                        cs.setStrokingColor(Color.BLACK);
                        cs.beginText();
                        cs.setFont(new PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), fontSize);
                        cs.newLineAtOffset(x, yStart);

                        float used = 0f;
                        for (int i = 0; i < translatedLines.size(); i++) {
                            if (lineToPage.get(i) != pi) continue;
                            String line = translatedLines.get(i);
                            // 基础换行：避免过长行溢出，按固定列宽切割
                            int maxCols = 80;
                            int idx = 0;
                            while (idx < line.length()) {
                                int end = Math.min(idx + maxCols, line.length());
                                String chunk = line.substring(idx, end);
                                cs.showText(chunk);
                                cs.newLineAtOffset(0, -leading); // 向下移动，保持从上到下顺序
                                used += leading;
                                // 到达页底则停止当前页写入
                                if (used > (yStart - margin)) break;
                                idx = end;
                            }
                            if (used > (yStart - margin)) break;
                        }

                        cs.endText();
                    }
                }

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    newDoc.save(baos);
                    progressCallback.accept(90, "新PDF生成完成，准备上传");
                    return baos.toByteArray();
                }
            }
        }
    }

    /**
     * 判断是否为“数字样式”文本：仅由空白、数字及常见分隔符构成，不需要翻译。
     */
    private static boolean isNumberLike(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        return t.matches("[\\s0-9.,:;\\-+()/%]+");
    }

    // 旧的基于内容流 Token 的替换实现已移除，改为 TextStripper 提取 + 叠加写入，减少乱码风险。
}