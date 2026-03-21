package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 *
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum implements BaseEnum<Integer> {

    /**
     * 文本消息
     */
    TEXT(1, "文本消息"),

    /**
     * 图片消息
     */
    IMAGE(2, "图片消息"),

    /**
     * 文件消息
     */
    FILE(3, "文件消息"),

    /**
     * 语音消息
     */
    VOICE(4, "语音消息"),

    /**
     * 系统消息
     */
    SYSTEM(5, "系统消息");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}