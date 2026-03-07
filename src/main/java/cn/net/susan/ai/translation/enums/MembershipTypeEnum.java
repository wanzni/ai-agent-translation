package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会员类型枚举（统一后缀 Enum）
 *
 * 用于描述会员套餐的时长与类型，统一用于订单、订阅、控制器入参等场景。
 * 枚举值说明：
 * - MONTHLY：包月，1 个月时长；
 * - QUARTERLY：包季，3 个月时长；
 * - YEARLY：包年，12 个月时长。
 */
@Getter
@AllArgsConstructor
public enum MembershipTypeEnum implements BaseEnum<Integer> {
    /**
     * 包月会员（1个月）
     */
    MONTHLY(1, "包月会员"),
    /**
     * 包季会员（3个月）
     */
    QUARTERLY(2, "包季会员"),
    /**
     * 包年会员（12个月）
     */
    YEARLY(3, "包年会员");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;
}