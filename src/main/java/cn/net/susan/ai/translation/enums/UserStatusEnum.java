package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author 苏三
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum implements BaseEnum<Integer>{

    /**
     * 活跃
     */
    ACTIVE(1, "活跃"),

    /**
     * 非活跃
     */
    INACTIVE(2, "非活跃"),

    /**
     * 已暂停
     */
    SUSPENDED(3, "已暂停");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}