package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情响应
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 账户ID
     */
    private String accountId;

    /**
     * 账户名
     */
    private String accountName;

    /**
     * 类型
     */
    private String type;

    /**
     * 月份
     */
    private Integer months;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 货币
     */
    private String currency;

    /**
     * 状态
     */
    private String status;

    /**
     * 周期配额
     */
    private Long periodQuota;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 开始时间
     */
    private LocalDateTime startAt;

    /**
     * 结束时间
     */
    private LocalDateTime endAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
}