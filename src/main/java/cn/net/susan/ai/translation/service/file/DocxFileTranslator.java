package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * DOCX 翻译器：按文本 run 级别进行批量翻译，保留原字体与样式，仅替换文字内容。
 */
@Slf4j
public class DocxFileTranslator implements DocumentFileTranslator {

    @Override
    public byte[] translate(byte[] sourceBytes,
                             DocumentTranslation task,
                             TranslationService translationService,
                             BiConsumer<Integer, String> progressCallback) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(sourceBytes);
             XWPFDocument document = new XWPFDocument(bais)) {

            List<XWPFRun> targetRuns = new ArrayList<>();
            List<TranslationRequest> requests = new ArrayList<>();

            // 遍历文档主体与表格中的段落与run
            int scanned = 0;
            for (IBodyElement bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof XWPFParagraph) {
                    collectRuns((XWPFParagraph) bodyElement, task, targetRuns, requests);
                } else if (bodyElement instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) bodyElement;
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph p : cell.getParagraphs()) {
                                collectRuns(p, task, targetRuns, requests);
                            }
                        }
                    }
                }
                scanned++;
                if (scanned % 10 == 0) {
                    progressCallback.accept(Math.min(50, 20 + scanned), "扫描段落/表格中(" + scanned + ")");
                }
            }

            if (requests.isEmpty()) {
                progressCallback.accept(60, "DOCX未发现可翻译文本，原样输出");
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    document.write(baos);
                    return baos.toByteArray();
                }
            }

            progressCallback.accept(55, "批量翻译中，共" + requests.size() + "个文字段");
            List<TranslationResponse> responses = translationService.batchTranslate(requests);

            int applied = 0;
            for (int i = 0; i < targetRuns.size(); i++) {
                XWPFRun run = targetRuns.get(i);
                String translated = null;
                if (responses != null && i < responses.size() && responses.get(i) != null) {
                    translated = responses.get(i).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    try {
                        // 单条重试，避免批量返回空导致残留原文或出现标签
                        TranslationResponse retry = translationService.translate(requests.get(i));
                        if (retry != null && retry.getTranslatedText() != null && !retry.getTranslatedText().isBlank()) {
                            translated = retry.getTranslatedText();
                        }
                    } catch (Exception ignore) {
                    }
                    // 仍为空则保留原文但不添加语言标签
                    if (translated == null || translated.isBlank()) {
                        translated = requests.get(i).getSourceText();
                    }
                }
                // 仅替换内容，不调整样式
                replaceRunText(run, translated);
                applied++;
                if (applied % 200 == 0) {
                    int percent = 60 + (int) Math.round(applied * 25.0 / requests.size());
                    progressCallback.accept(Math.min(85, percent), "已替换译文(" + applied + "/" + requests.size() + ")");
                }
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.write(baos);
                progressCallback.accept(90, "DOCX内容处理完成，准备上传");
                return baos.toByteArray();
            }
        }
    }

    private void collectRuns(XWPFParagraph paragraph,
                              DocumentTranslation task,
                              List<XWPFRun> targetRuns,
                              List<TranslationRequest> requests) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null) return;
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null && !text.trim().isEmpty()) {
                targetRuns.add(run);
                requests.add(TranslationRequest.builder()
                        .sourceText(text)
                        .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                        .targetLanguage(task.getTargetLanguage())
                        .translationType("TEXT")
                        .translationEngine(task.getTranslationEngine())
                        .build());
            }
        }
    }

    private void replaceRunText(XWPFRun run, String translated) {
        // 清空当前 run 的第一个文本并写入译文，保持 run 样式不变
        run.setText("", 0);
        run.setText(translated, 0);
    }
}