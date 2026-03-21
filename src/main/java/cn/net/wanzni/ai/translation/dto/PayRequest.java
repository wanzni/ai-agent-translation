package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import cn.net.wanzni.ai.translation.enums.PaymentProviderEnum;
import cn.net.wanzni.ai.translation.enums.PaymentMethodEnum;

/**
 * 支付请求 DTO
 *
 * 用于发起订单支付，包含订单号、支付渠道与支付方式。
 * 控制器接收后将调用服务层进行支付记录写入与订单状态更新。
 */
@Data
public class PayRequest {
    /** 订单号（MembershipOrder.orderNo） */
    private String orderNo;
    /** 支付渠道（如 ALIPAY/WECHAT/STRIPE/PAYPAL/OTHER） */
    private PaymentProviderEnum provider;
    /** 支付方式（如 ALIPAY_QR/WECHAT_APP/CREDIT_CARD 等） */
    private PaymentMethodEnum method;
}