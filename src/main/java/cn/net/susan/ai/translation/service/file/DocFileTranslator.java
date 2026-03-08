package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.CharacterRun;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * DOC 翻译器：遍历段落的字符运行（CharacterRun）进行批量翻译，尽量保留样式。
 * 说明：旧版 DOC 文档结构复杂，采用逐 run 替换文本以最大程度保留字体与样式；
 * 对富文本场景（同 run 内多样式）保留度受限于 HWPF 能力。
 */
@Slf4j
public class DocFileTranslator implements DocumentFileTranslator {

    @Override
    public byte[] translate(byte[] sourceBytes,
                             DocumentTranslation task,
                             TranslationService translationService,
                             BiConsumer<Integer, String> progressCallback) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(sourceBytes);
             HWPFDocument document = new HWPFDocument(bais)) {

            Range range = document.getRange();
            List<RunSlot> targetRuns = new ArrayList<>();
            List<TranslationRequest> requests = new ArrayList<>();

            int paraCount = range.numParagraphs();
            int scanned = 0;
            for (int pi = 0; pi < paraCount; pi++) {
                Paragraph p = range.getParagraph(pi);
                int runs = p.numCharacterRuns();
                for (int ri = 0; ri < runs; ri++) {
                    CharacterRun cr = p.getCharacterRun(ri);
                    String text = sanitize(cr.text());
                    if (text != null && !text.trim().isEmpty()) {
                        targetRuns.add(new RunSlot(p, cr, text));
                        requests.add(TranslationRequest.builder()
                                .sourceText(text)
                                .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                                .targetLanguage(task.getTargetLanguage())
                                .translationType("TEXT")
                                .translationEngine(task.getTranslationEngine())
                                .build());
                    }
                }
                scanned++;
                if (scanned % 50 == 0) {
                    progressCallback.accept(Math.min(50, 20 + scanned / 2), "扫描段落中(" + scanned + ")");
                }
            }

            if (requests.isEmpty()) {
                progressCallback.accept(60, "DOC未发现可翻译文本，原样输出");
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    document.write(baos);
                    return baos.toByteArray();
                }
            }

            progressCallback.accept(55, "并行翻译中，共" + requests.size() + "个文字段");
            List<TranslationResponse> responses = translationService.parallelBatchTranslate(requests);

            int applied = 0;
            for (int i = 0; i < targetRuns.size(); i++) {
                RunSlot slot = targetRuns.get(i);
                String translated = null;
                if (responses != null && i < responses.size() && responses.get(i) != null) {
                    translated = responses.get(i).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    try {
                        TranslationResponse retry = translationService.translate(requests.get(i));
                        if (retry != null && retry.getTranslatedText() != null && !retry.getTranslatedText().isBlank()) {
                            translated = retry.getTranslatedText();
                        }
                    } catch (Exception ignore) {
                    }
                    if (translated == null || translated.isBlank()) {
                        translated = requests.get(i).getSourceText();
                    }
                }
                // 在段落范围内替换该 run 的原文本，尽量保持样式
                slot.paragraph.replaceText(slot.originalText, translated);
                applied++;
                if (applied % 200 == 0) {
                    int percent = 60 + (int) Math.round(applied * 25.0 / requests.size());
                    progressCallback.accept(Math.min(85, percent), "已替换译文(" + applied + "/" + requests.size() + ")");
                }
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.write(baos);
                progressCallback.accept(90, "DOC内容处理完成，准备上传");
                return baos.toByteArray();
            }
        }
    }

    private String sanitize(String s) {
        if (s == null) return null;
        // 去除常见控制字符（如 \r, \n, 0x07 等）以避免 run 边界噪声影响翻译
        return s.replace("\r", " ").replace("\n", " ").replace("\u0007", "").trim();
    }

    private static class RunSlot {
        final Paragraph paragraph;
        final CharacterRun run;
        final String originalText;

        RunSlot(Paragraph paragraph, CharacterRun run, String originalText) {
            this.paragraph = paragraph;
            this.run = run;
            this.originalText = originalText;
        }
    }
}