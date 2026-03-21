package cn.net.wanzni.ai.translation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cn.net.wanzni.ai.translation.enums.PaymentProviderEnum;
import cn.net.wanzni.ai.translation.enums.PaymentMethodEnum;
import cn.net.wanzni.ai.translation.enums.PaymentStatusEnum;

/**
 * 支付记录实体（PaymentRecord）
 *
 * 用于持久化每次订单支付的流水信息，支持多渠道、多方式与状态变更。
 * 字段含义与业务约定：
 * - provider/method：标识支付渠道与方式；
 * - status：记录支付过程状态与结果；
 * - transactionNo：三方支付平台返回的交易号；
 * - notifyPayload：三方回调的原始报文，用于对账与审计；
 * - errorMessage：失败或异常的友好信息提示；
 * - createdAt/updatedAt：自动维护的时间戳。
 */
@Entity
@Table(name = "payment_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联订单主键ID（MembershipOrder.id） */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 支付单号（系统生成，唯一） */
    @Column(name = "payment_no", nullable = false, length = 64, unique = true)
    private String paymentNo;

    /** 支付渠道（如 ALIPAY/WECHAT/STRIPE/PAYPAL/OTHER） */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "provider", nullable = false)
    private PaymentProviderEnum provider;

    /** 支付方式（如 ALIPAY_QR/WECHAT_APP/CREDIT_CARD 等） */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "method", nullable = false)
    private PaymentMethodEnum method;

    /** 支付金额 */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 币种（默认 CNY） */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "CNY";

    /** 支付状态（INITIATED/SUCCESS/FAILED/REFUNDED/CANCELLED） */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatusEnum status = PaymentStatusEnum.INITIATED;

    /** 三方交易流水号（回调/查询返回） */
    @Column(name = "transaction_no", length = 128)
    private String transactionNo;

    /** 支付成功时间 */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 支付回调原始报文（JSON/XML 等）。
     * 为避免长度限制，使用 LOB 存储，便于审计与对账。
     */
    @Lob
    @Column(name = "notify_payload")
    private String notifyPayload;

    /** 错误信息（当状态为 FAILED/CANCELLED 等时记录） */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 创建时间（自动维护） */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间（自动维护） */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}