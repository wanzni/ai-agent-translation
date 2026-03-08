package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * TXT 翻译器：按行拆分进行批量翻译，保持空行与基本结构。
 */
@Slf4j
public class TxtFileTranslator implements DocumentFileTranslator {

    @Override
    public byte[] translate(byte[] sourceBytes,
                             DocumentTranslation task,
                             TranslationService translationService,
                             BiConsumer<Integer, String> progressCallback) throws Exception {
        Charset charset = StandardCharsets.UTF_8;
        String original = new String(sourceBytes, charset);

        String[] lines = original.split("\r\n|\n|\r", -1);
        int total = lines.length;

        // 构建批量请求，跳过空行，仅翻译非空内容
        List<TranslationRequest> requests = new ArrayList<>();
        int scanned = 0;
        for (String line : lines) {
            scanned++;
            if (line != null && !line.trim().isEmpty()) {
                requests.add(TranslationRequest.builder()
                        .sourceText(line)
                        .sourceLanguage(task.getSourceLanguage() == null ? "auto" : task.getSourceLanguage())
                        .targetLanguage(task.getTargetLanguage())
                        .translationType("TEXT")
                        .translationEngine(task.getTranslationEngine())
                        .build());
            }
            int percent = Math.min(45, 20 + scanned * 2);
            progressCallback.accept(percent, "扫描文本中(" + scanned + "/" + total + ")");
        }

        if (requests.isEmpty()) {
            progressCallback.accept(60, "TXT未发现可翻译文本，原样输出");
            return sourceBytes;
        }

        progressCallback.accept(50, "并行翻译中，共" + requests.size() + "段");
            List<TranslationResponse> responses = translationService.parallelBatchTranslate(requests);

        // 写回译文，保留空行；对空结果降级为带目标语言标签的原文
        List<String> outLines = new ArrayList<>(total);
        int applied = 0;
        int respIdx = 0;
        for (int i = 0; i < total; i++) {
            String line = lines[i];
            String out = line;
            if (line != null && !line.trim().isEmpty()) {
                String translated = null;
                if (responses != null && respIdx < responses.size() && responses.get(respIdx) != null) {
                    translated = responses.get(respIdx).getTranslatedText();
                }
                if (translated == null || translated.isBlank()) {
                    translated = "[" + task.getTargetLanguage() + "] " + line;
                }
                out = translated;
                respIdx++;
                applied++;
            }
            outLines.add(out);
            int percent = 55 + (int) Math.round(applied * 30.0 / requests.size());
            progressCallback.accept(Math.min(85, percent), "已替换译文(" + applied + "/" + requests.size() + ")");
        }

        String joined = String.join("\n", outLines);
        progressCallback.accept(90, "内容处理完成，准备上传");
        return joined.getBytes(charset);
    }
}