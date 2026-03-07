package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会员状态枚举
 *
 * 表示用户会员资格的生命周期状态。
 */
@Getter
@AllArgsConstructor
public enum MembershipStatusEnum implements BaseEnum<Integer> {
    /** 正常生效中 */
    ACTIVE(1, "正常生效中"),
    /** 已过期 */
    EXPIRED(2, "已过期"),
    /** 已取消 */
    CANCELLED(3, "已取消");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;
}