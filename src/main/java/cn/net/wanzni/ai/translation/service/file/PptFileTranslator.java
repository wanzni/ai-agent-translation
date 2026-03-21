package cn.net.wanzni.ai.translation.service.file;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hslf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * PPT 翻译器（HSLF）：按 RichTextRun 级别进行批量翻译，保留原字体与样式，仅替换文字内容。
 */
@Slf4j
public class PptFileTranslator implements DocumentFileTranslator {

    private static class RunSlot {
        final HSLFTextRun run;
        final String original;
        RunSlot(HSLFTextRun run, String original) { this.run = run; this.original = original; }
    }

    /**
     * 计算文本中最长的连续英数字串长度，用于判断英文字符密度。
     */
    private static int longestAsciiWordRun(String s) {
        if (s == null || s.isEmpty()) return 0;
        int longest = 0, cur = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                cur++;
                if (cur > longest) longest = cur;
            } else {
                cur = 0;
            }
        }
        return longest;
    }

    @Override
    public byte[] translate(byte[] sourceBytes,
                             DocumentTranslation task,
                             TranslationService translationService,
                             BiConsumer<Integer, String> progressCallback) throws Exception {
        try (HSLFSlideShow ppt = new HSLFSlideShow(new ByteArrayInputStream(sourceBytes))) {
            List<RunSlot> slots = new ArrayList<>();

            int scannedSlides = 0;
            for (HSLFSlide slide : ppt.getSlides()) {
                scannedSlides++;
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        List<HSLFTextParagraph> paragraphs = textShape.getTextParagraphs();
                        if (paragraphs == null) continue;
                        for (HSLFTextParagraph p : paragraphs) {
                            for (HSLFTextRun r : p.getTextRuns()) {
                                String text = r.getRawText();
                                if (text != null && !text.trim().isEmpty()) {
                                    slots.add(new RunSlot(r, text));
                                }
                            }
                        }
                    }
                }
                if (scannedSlides % 3 == 0) {
                    progressCallback.accept(Math.min(40, 20 + scannedSlides * 3),
                            "扫描文本中，第" + scannedSlides + "页");
                }
            }

            if (slots.isEmpty()) {
                log.info("PPT未发现可翻译文本，原样输出");
                return sourceBytes;
            }

            List<TranslationRequest> requests = new ArrayList<>(slots.size());
            for (RunSlot s : slots) {
                requests.add(TranslationRequest.builder()
                        .sourceText(s.original)
                        .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                        .targetLanguage(task.getTargetLanguage())
                        .translationType("TEXT")
                        .translationEngine(task.getTranslationEngine())
                        .build());
            }

            progressCallback.accept(50, "并行翻译中，共" + requests.size() + "段");
            List<TranslationResponse> responses = translationService.parallelBatchTranslate(requests);

            int applied = 0;
            for (int i = 0; i < slots.size(); i++) {
                RunSlot slot = slots.get(i);
                String translated = null;
                if (responses != null && i < responses.size() && responses.get(i) != null) {
                    translated = responses.get(i).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    translated = "[" + task.getTargetLanguage() + "] " + slot.original;
                }
                // 直接在 RichTextRun 上替换文本，不改样式
                slot.run.setText(translated);

                applied++;
                if (applied % 200 == 0) {
                    int percent = 55 + (int) Math.round(applied * 30.0 / slots.size());
                    progressCallback.accept(Math.min(85, percent), "已替换译文(" + applied + "/" + slots.size() + ")");
                }
            }

            // 翻译后进行格式调整：启用自动换行、设置合理行距、并依据英文密度动态扩展文本框宽度，避免文字重叠
            progressCallback.accept(86, "正在调整PPT文本格式以避免重叠");
            for (HSLFSlide slide : ppt.getSlides()) {
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        try {
                            textShape.setWordWrap(true);
                            List<HSLFTextParagraph> paragraphs = textShape.getTextParagraphs();
                            if (paragraphs != null) {
                                for (HSLFTextParagraph p : paragraphs) {
                                    p.setLineSpacing(120.0);
                                }
                            }
                            // 根据文本中英文密度动态拉宽文本框，让一行尽可能多显示字符
                            java.awt.Dimension pageSize = ppt.getPageSize();
                            java.awt.geom.Rectangle2D anchor = textShape.getAnchor();
                            int rightMargin = 20;
                            double maxWidth = Math.max(50.0, pageSize.getWidth() - anchor.getX() - rightMargin);

                            String fullText = textShape.getText();
                            int longestRun = longestAsciiWordRun(fullText);
                            double multiplier;
                            if (longestRun >= 20) {
                                multiplier = 1.9;
                            } else if (longestRun >= 12) {
                                multiplier = 1.7;
                            } else if (longestRun >= 8) {
                                multiplier = 1.55;
                            } else {
                                multiplier = 1.35;
                            }

                            double targetWidth = Math.min(anchor.getWidth() * multiplier, maxWidth);
                            if (targetWidth > anchor.getWidth()) {
                                textShape.setAnchor(new java.awt.geom.Rectangle2D.Double(
                                        anchor.getX(), anchor.getY(), targetWidth, anchor.getHeight()
                                ));
                            }
                            textShape.resizeToFitText();
                        } catch (Exception e) {
                            log.debug("调整文本框格式失败，跳过该形状", e);
                        }
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ppt.write(baos);
            progressCallback.accept(90, "内容处理完成，准备上传");
            return baos.toByteArray();
        }
    }
}