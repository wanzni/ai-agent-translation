package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易类型枚举
 *
 * @author 苏三
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum TransactionTypeEnum implements BaseEnum<Integer> {

    /**
     * 扣除
     */
    DEDUCT(1, "扣除"),

    /**
     * 增加
     */
    ADD(2, "增加"),

    /**
     * 奖励
     */
    BONUS(3, "奖励"),

    /**
     * 退款
     */
    REFUND(4, "退款"),

    /**
     * 调整
     */
    ADJUST(5, "调整");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}