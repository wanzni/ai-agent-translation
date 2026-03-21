package cn.net.wanzni.ai.translation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import cn.net.wanzni.ai.translation.enums.MembershipTypeEnum;
import cn.net.wanzni.ai.translation.enums.MembershipStatusEnum;

/**
 * 用户会员实体（UserMembership）
 *
 * 描述用户的会员资格、生效区间与周期配额信息。
 * 字段与约定：
 * - type：会员类型（统一使用 MembershipTypeEnum）；
 * - status：会员状态（ACTIVE/EXPIRED/CANCELLED）；
 * - periodQuota/periodUsed：周期配额与已使用值；
 * - startAt/endAt：会员生效区间。
 */
@Entity
@Table(name = "user_memberships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type", nullable = false)
    private MembershipTypeEnum type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MembershipStatusEnum status = MembershipStatusEnum.ACTIVE;

    /** 会员开始时间 */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /** 会员结束时间 */
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /** 周期配额（当期可用点数上限） */
    @Column(name = "period_quota", nullable = false)
    @Builder.Default
    private Long periodQuota = 0L;

    /** 周期内已使用点数 */
    @Column(name = "period_used", nullable = false)
    @Builder.Default
    private Long periodUsed = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isActiveNow() {
        return this.status == MembershipStatusEnum.ACTIVE && endAt != null && endAt.isAfter(LocalDateTime.now());
    }

    public long remainingQuota() {
        return Math.max(0L, (periodQuota == null ? 0L : periodQuota) - (periodUsed == null ? 0L : periodUsed));
    }
}