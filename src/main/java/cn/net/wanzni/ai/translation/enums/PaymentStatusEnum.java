package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举（Payment Status）
 *
 * 表示支付记录的生命周期状态，用于对账与业务流转。
 */
@Getter
@AllArgsConstructor
public enum PaymentStatusEnum implements BaseEnum<Integer> {
    /**
     * 已创建，待支付/待回调
     */
    INITIATED(1, "待支付"),
    /**
     * 支付成功
     */
    SUCCESS(2, "支付成功"),
    /**
     * 支付失败
     */
    FAILED(3, "支付失败"),
    /**
     * 已退款
     */
    REFUNDED(4, "已退款"),
    /**
     * 已取消
     */
    CANCELLED(5, "已取消");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;
}