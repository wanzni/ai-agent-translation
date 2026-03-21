package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道枚举（Payment Provider）
 *
 * 表示发起支付的渠道或第三方平台来源。
 * 仅作为渠道标识，不涉及具体支付能力的差异实现。
 */
@Getter
@AllArgsConstructor
public enum PaymentProviderEnum implements BaseEnum<Integer> {
    /**
     * 支付宝
     */
    ALIPAY(1, "支付宝"),
    /**
     * 微信支付
     */
    WECHAT(2, "微信支付"),
    /**
     * 银联云闪付
     */
    UNIONPAY(3, "银联云闪付"),
    /**
     * Stripe 国际支付平台
     */
    STRIPE(4, "Stripe"),
    /**
     * PayPal 国际支付平台
     */
    PAYPAL(5, "PayPal"),
    /**
     * 其他渠道（用于占位或扩展）
     */
    OTHER(6, "其他渠道");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;
}