package cn.net.wanzni.ai.translation.entity;

import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_task_id", nullable = false)
    private Long agentTaskId;

    @Column(name = "biz_type", length = 64)
    private String bizType;

    @Column(name = "biz_id", length = 64)
    private String bizId;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Lob
    @Column(name = "issue_summary", columnDefinition = "TEXT")
    private String issueSummary;

    @Lob
    @Column(name = "suggested_text", columnDefinition = "LONGTEXT")
    private String suggestedText;

    @Lob
    @Column(name = "final_text", columnDefinition = "LONGTEXT")
    private String finalText;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    @Builder.Default
    private ReviewStatusEnum reviewStatus = ReviewStatusEnum.PENDING;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
