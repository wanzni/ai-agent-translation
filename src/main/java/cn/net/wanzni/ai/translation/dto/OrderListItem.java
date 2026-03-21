package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItem {
    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private String userId;

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
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
}