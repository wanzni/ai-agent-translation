package cn.net.wanzni.ai.translation.dto;

import cn.net.wanzni.ai.translation.enums.PaymentMethodEnum;
import cn.net.wanzni.ai.translation.enums.PaymentProviderEnum;
import lombok.Data;

/**
 * 扫码预下单请求
 *
 * @version 1.0.0
 */
@Data
public class PaymentPrepayRequest {
    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付渠道（ALIPAY/WECHAT）
     */
    private PaymentProviderEnum provider;

    /**
     * 支付方式（ALIPAY_QR/WECHAT_QR）
     */
    private PaymentMethodEnum method;
}