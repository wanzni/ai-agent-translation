package cn.net.susan.ai.translation.dto;

import cn.net.susan.ai.translation.enums.PaymentMethodEnum;
import cn.net.susan.ai.translation.enums.PaymentProviderEnum;
import lombok.Data;

/**
 * 扫码预下单请求
 *
 * @author 苏三
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