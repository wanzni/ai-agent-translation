package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 *
 * @author 苏三
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum implements BaseEnum<Integer> {

    /**
     * 系统通知
     */
    SYSTEM(1, "系统通知"),

    /**
     * 翻译相关
     */
    TRANSLATION(2, "翻译相关"),

    /**
     * 质量评估
     */
    QUALITY(3, "质量评估"),

    /**
     * 术语库
     */
    TERMINOLOGY(4, "术语库"),

    /**
     * 系统维护
     */
    MAINTENANCE(5, "系统维护"),

    /**
     * 版本更新
     */
    UPDATE(6, "版本更新"),

    /**
     * 安全提醒
     */
    SECURITY(7, "安全提醒");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}