package cn.net.wanzni.ai.translation.entity;

import cn.net.wanzni.ai.translation.enums.TransactionTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "points_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionTypeEnum type;

    /**
     * 变动点数（正为增加，负为扣减）
     */
    @Column(name = "delta", nullable = false)
    private Long delta;

    /** 引用业务ID，如翻译记录ID或任务ID */
    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reason", length = 200)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}