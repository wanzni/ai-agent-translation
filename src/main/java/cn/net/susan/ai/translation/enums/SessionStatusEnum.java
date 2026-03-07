package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话状态枚举
 *
 * @author 苏三
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum SessionStatusEnum implements BaseEnum<Integer> {

    /**
     * 活跃
     */
    ACTIVE(1, "活跃"),

    /**
     * 非活跃
     */
    INACTIVE(2, "非活跃"),

    /**
     * 已结束
     */
    ENDED(3, "已结束"),

    /**
     * 已归档
     */
    ARCHIVED(4, "已归档");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}