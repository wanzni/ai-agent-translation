package cn.net.susan.ai.translation.service.file;

import cn.net.susan.ai.translation.entity.DocumentTranslation;
import cn.net.susan.ai.translation.service.TranslationService;

import java.util.function.BiConsumer;

/**
 * 默认翻译器：不处理内容，原样复制（用于暂不支持的类型）。
 */
public class DefaultCopyTranslator implements DocumentFileTranslator {
    @Override
    public byte[] translate(byte[] sourceBytes, DocumentTranslation task, TranslationService translationService, BiConsumer<Integer, String> progressCallback) throws Exception {
        progressCallback.accept(80, "当前类型暂不支持内容替换，原样复制");
        return sourceBytes;
    }
}