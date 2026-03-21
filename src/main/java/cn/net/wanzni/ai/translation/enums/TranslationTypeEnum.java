package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 翻译类型枚举
 *
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum TranslationTypeEnum implements BaseEnum<Integer> {

    /**
     * 标准翻译
     */
    STANDARD(1, "标准翻译"),

    /**
     * 专业翻译
     */
    PROFESSIONAL(2, "专业翻译"),

    /**
     * 快速翻译
     */
    FAST(3, "快速翻译"),

    /**
     * 精准翻译
     */
    ACCURATE(4, "精准翻译");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}