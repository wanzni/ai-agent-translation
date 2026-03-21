package cn.net.wanzni.ai.translation.dto;

import lombok.Data;

/**
 * 确认支付请求（模拟三方回调）
 *
 * @version 1.0.0
 */
@Data
public class PaymentConfirmRequest {
    /**
     * 支付单号
     */
    private String paymentNo;
}