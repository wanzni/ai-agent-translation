package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * PPTX 翻译器：批量提取文本并调用翻译服务，尽量保留样式。
 */
@Slf4j
public class PptxFileTranslator implements DocumentFileTranslator {

    private static class RunEntry {
        final XSLFTextRun run;
        final String original;
        RunEntry(XSLFTextRun run, String original) { this.run = run; this.original = original; }
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
    public byte[] translate(byte[] sourceBytes, DocumentTranslation task, TranslationService translationService, BiConsumer<Integer, String> progressCallback) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(sourceBytes))) {
            List<RunEntry> entries = new ArrayList<>();

            int slideIdx = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                slideIdx++;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        for (XSLFTextParagraph p : textShape.getTextParagraphs()) {
                            for (XSLFTextRun r : p.getTextRuns()) {
                                String t = r.getRawText();
                                if (t != null && !t.trim().isEmpty()) {
                                    entries.add(new RunEntry(r, t));
                                }
                            }
                        }
                    }
                }
                progressCallback.accept(Math.min(40, 20 + slideIdx * 3), "扫描文本中，第" + slideIdx + "页");
            }

            if (entries.isEmpty()) {
                log.info("PPTX未发现可翻译文本，原样输出");
                return sourceBytes;
            }

            // 构建批量翻译请求
            List<TranslationRequest> requests = new ArrayList<>(entries.size());
            for (RunEntry e : entries) {
                requests.add(TranslationRequest.builder()
                        .sourceText(e.original)
                        .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                        .targetLanguage(task.getTargetLanguage())
                        .translationType("TEXT")
                        .translationEngine(task.getTranslationEngine())
                        .build());
            }

            progressCallback.accept(50, "批量翻译中，共" + requests.size() + "段");
            List<TranslationResponse> responses = translationService.batchTranslate(requests);

            int applied = 0;
            for (int i = 0; i < entries.size(); i++) {
                RunEntry entry = entries.get(i);
                String translated = null;
                if (responses != null && i < responses.size() && responses.get(i) != null) {
                    translated = responses.get(i).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    // 回退，避免中文原样输出
                    translated = "[" + task.getTargetLanguage() + "] " + entry.original;
                }
                // 直接在原 run 上替换文本，样式不变
                entry.run.setText(translated);

                applied++;
                int percent = 55 + (int) Math.round(applied * 30.0 / entries.size()); // 55%→85%
                progressCallback.accept(Math.min(85, percent), "已替换译文(" + applied + "/" + entries.size() + ")");
            }

            // 翻译后进行格式调整：启用自动换行、设置合理行距、并根据英文密度动态扩展文本框宽度，避免文字重叠
            progressCallback.accept(86, "正在调整PPT文本格式以避免重叠");
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        try {
                            // 开启换行，防止一行过长导致溢出
                            textShape.setWordWrap(true);
                            // 设置段落行距为 120%（POI 文档推荐用法），减少换行后行间拥挤
                            for (XSLFTextParagraph p : textShape.getTextParagraphs()) {
                                p.setLineSpacing(120.0);
                            }
                            // 根据文本中英文密度动态拉宽文本框，尽量让一行显示更多字符
                            java.awt.Dimension pageSize = ppt.getPageSize();
                            java.awt.geom.Rectangle2D anchor = textShape.getAnchor();
                            double rightMargin = 20.0;
                            double maxWidth = Math.max(50.0, pageSize.getWidth() - anchor.getX() - rightMargin);

                            String fullText = textShape.getText();
                            int longestRun = longestAsciiWordRun(fullText);
                            double multiplier;
                            if (longestRun >= 20) {
                                multiplier = 1.9; // 超长英文单词，尽量加宽
                            } else if (longestRun >= 12) {
                                multiplier = 1.7; // 较长英文词，显著加宽
                            } else if (longestRun >= 8) {
                                multiplier = 1.55; // 中等长度英文，适度加宽
                            } else {
                                multiplier = 1.35; // 默认加宽
                            }

                            double targetWidth = Math.min(anchor.getWidth() * multiplier, maxWidth);
                            if (targetWidth > anchor.getWidth()) {
                                textShape.setAnchor(new java.awt.geom.Rectangle2D.Double(
                                        anchor.getX(), anchor.getY(), targetWidth, anchor.getHeight()
                                ));
                            }
                            // 内容超出当前高度时，扩展文本框以完整容纳文本
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