package cn.net.wanzni.ai.translation.service.file;

import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.service.TranslationService;

import java.util.function.BiConsumer;

/**
 * 文档类型翻译策略接口：每种文件类型一个实现类。
 */
public interface DocumentFileTranslator {

    /**
     * 翻译文档字节内容，尽量保持原样式。
     * @param sourceBytes 原文档字节
     * @param task 任务信息（含语言、引擎等）
     * @param translationService 文本翻译服务
     * @param progressCallback 进度回调（百分比, 消息）
     * @return 翻译后的字节
     */
    byte[] translate(byte[] sourceBytes,
                     DocumentTranslation task,
                     TranslationService translationService,
                     BiConsumer<Integer, String> progressCallback) throws Exception;
}