package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * 标识订单在生命周期中的状态，用于控制支付与取消等流程。
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum implements BaseEnum<Integer> {
    /**
     * 待支付（可支付/可取消）
     */
    PENDING(1, "待支付"),
    /**
     * 已支付（不可再支付）
     */
    PAID(2, "已支付"),
    /**
     * 已取消（超时或主动取消）
     */
    CANCELLED(3, "已取消"),
    /**
     * 已退款（支付后退款完成）
     */
    REFUNDED(4, "已退款");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;
}