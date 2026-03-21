package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 处理状态枚举
 *
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum ProcessingStatusEnum implements BaseEnum<Integer> {

    /**
     * 待处理
     */
    PENDING(1, "待处理"),

    /**
     * 上传中
     */
    UPLOADING(2, "上传中"),

    /**
     * 处理中
     */
    PROCESSING(3, "处理中"),

    /**
     * 已暂停
     */
    PAUSED(4, "已暂停"),

    /**
     * 已完成
     */
    COMPLETED(5, "已完成"),

    /**
     * 失败
     */
    FAILED(6, "失败"),

    /**
     * 已取消
     */
    CANCELLED(7, "已取消");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}