package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建订单后的响应，包含订单的详细信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    /** 订单号 */
    private String orderNo;
    /** 用户ID */
    private Long userId;
    /** 会员类型 */
    private String type;
    /** 订单包含的会员月数 */
    private Integer months;
    /** 订单金额 */
    private BigDecimal amount;
    /** 货币单位 */
    private String currency;
    /** 订单包含的配额 */
    private Long periodQuota;
    /** 订单创建时间 */
    private LocalDateTime createdAt;
    /** 订单过期时间 */
    private LocalDateTime expiresAt;
}