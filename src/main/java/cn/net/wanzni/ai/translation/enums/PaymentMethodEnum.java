package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举（Payment Method）
 *
 * 表示用户选择的支付方式或具体入口。例如二维码、App 内支付、银行卡等。
 */
@Getter
@AllArgsConstructor
public enum PaymentMethodEnum implements BaseEnum<Integer> {
    /**
     * 支付宝 App 内支付
     */
    ALIPAY_APP(1, "支付宝 App 内支付"),
    /**
     * 支付宝二维码支付
     */
    ALIPAY_QR(2, "支付宝二维码支付"),
    /**
     * 微信 App 内支付
     */
    WECHAT_APP(3, "微信 App 内支付"),
    /**
     * 微信二维码支付
     */
    WECHAT_QR(4, "微信二维码支付"),
    /**
     * 银联二维码支付（云闪付）
     */
    UNIONPAY_QR(5, "银联二维码支付"),
    /**
     * 银行卡（信用卡/借记卡）支付
     */
    CREDIT_CARD(6, "银行卡支付"),
    /**
     * 银行转账
     */
    BANK_TRANSFER(7, "银行转账"),
    /**
     * 其他支付方式（用于占位或扩展）
     */
    OTHER(8, "其他支付方式");

    /**
     * 枚举值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;
}