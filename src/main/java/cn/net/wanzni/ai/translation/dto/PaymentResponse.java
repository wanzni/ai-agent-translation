package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付结果响应 DTO
 *
 * 前端在支付成功后用于跳转与展示（包含订单与会员生效信息）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    /** 支付单号（系统生成） */
    private String paymentNo;
    /** 三方交易号（第三方平台返回） */
    private String transactionNo;
    /** 订单号 */
    private String orderNo;
    /** 支付成功时间 */
    private LocalDateTime paidAt;
    /** 金额 */
    private BigDecimal amount;
    /** 币种（如 CNY） */
    private String currency;
    /** 支付渠道（字符串名称） */
    private String provider;
    /** 支付状态（字符串名称） */
    private String status;
    /** 用户ID（字符串形式） */
    private Long userId;
    /** 会员类型（字符串枚举名） */
    private String type;
    /** 购买月份数（1、3、12） */
    private Integer months;
    /** 会员期内配额 */
    private Long periodQuota;
    /** 会员开始时间 */
    private LocalDateTime startAt;
    /** 会员结束时间 */
    private LocalDateTime endAt;
}