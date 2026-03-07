package cn.net.susan.ai.translation.service.translation;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.dto.LanguageDetectionResponse;

/**
 * 第三方翻译统一接口。
 * 所有外部（第三方）翻译能力实现都应实现该接口，
 * 并放置在 {@code cn.net.susan.ai.translation.provider} 目录下。
 */
public interface ThirdPartyTranslator {
    /**
     * 返回引擎标识（例如："ALIBABA_CLOUD"、"QWEN"）。
     */
    String getEngineCode();

    /**
     * 执行文本翻译。
     */
    TranslationResponse translate(TranslationRequest request) throws Exception;

    /**
     * 执行语言检测。
     */
    LanguageDetectionResponse detectLanguage(String text) throws Exception;
}