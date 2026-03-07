package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 扫码预下单响应
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPrepayResponse {
    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付单号（系统生成）
     */
    private String paymentNo;

    /**
     * 二维码链接（供前端生成二维码）
     */
    private String codeUrl;

    /**
     * 二维码过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 渠道（ALIPAY/WECHAT）
     */
    private String provider;

    /**
     * 方式（ALIPAY_QR/WECHAT_QR）
     */
    private String method;
}