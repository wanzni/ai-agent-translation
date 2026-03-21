package cn.net.wanzni.ai.translation.entity;

import cn.net.wanzni.ai.translation.enums.MembershipTypeEnum;
import cn.net.wanzni.ai.translation.enums.OrderStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员订单实体
 *
 * 描述会员购买订单的核心信息，包括类型、时长、金额、配额与状态等。
 * 使用统一的枚举命名后缀 Enum，并与支付记录等实体保持一致。
 * 字段说明：
 * - orderNo：订单唯一编号，便于外部对接与查询；
 * - userId：下单用户标识；
 * - membershipType：会员套餐类型（包月/包季/包年，使用 MembershipTypeEnum）；
 * - months：购买的时长（月数）；
 * - amount/currency：订单金额与币种（默认 CNY）；
 * - periodQuota：本周期（本次订阅周期）可用配额；
 * - status：订单状态（使用 OrderStatusEnum）；
 * - startAt/endAt：会员权益的起止时间；
 * - paidAt：支付完成时间；
 * - paymentCount/latestPaymentId：支付尝试次数与最后一次支付记录；
 * - externalTradeNo：外部支付流水号；
 * - description：备注信息；
 * - createdAt/updatedAt：创建与更新时间（自动维护）。
 */
@Entity
@Table(name = "membership_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "membership_type", nullable = false)
    /** 会员类型（统一枚举 MembershipTypeEnum） */
    private MembershipTypeEnum membershipType;

    @Column(name = "period_quota", nullable = false)
    private Long periodQuota;

    @Column(name = "months", nullable = false)
    private Integer months;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "CNY";

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatusEnum status = OrderStatusEnum.PENDING;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_count", nullable = false)
    @Builder.Default
    private Integer paymentCount = 0;

    @Column(name = "latest_payment_id")
    private Long latestPaymentId;

    @Column(name = "external_trade_no", length = 128)
    private String externalTradeNo;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 枚举迁移至 cn.net.wanzni.ai.translation.enums 包，移除实体内嵌枚举
}