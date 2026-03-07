package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 翻译状态枚举
 *
 * @author 苏三
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum TranslationStatusEnum implements BaseEnum<Integer> {

    /**
     * 待翻译
     */
    PENDING(1, "待翻译"),

    /**
     * 翻译中
     */
    TRANSLATING(2, "翻译中"),

    /**
     * 翻译完成
     */
    COMPLETED(3, "翻译完成"),

    /**
     * 翻译失败
     */
    FAILED(4, "翻译失败"),

    /**
     * 跳过翻译
     */
    SKIPPED(5, "跳过翻译");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}