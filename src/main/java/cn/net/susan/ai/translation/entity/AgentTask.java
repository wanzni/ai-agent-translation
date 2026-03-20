package cn.net.susan.ai.translation.entity;

import cn.net.susan.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.susan.ai.translation.enums.AgentTaskTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, unique = true, length = 64)
    private String taskNo;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 32)
    private AgentTaskTypeEnum taskType;

    @Column(name = "biz_type", length = 64)
    private String bizType;

    @Column(name = "biz_id", length = 64)
    private String bizId;

    @Column(name = "source_language", nullable = false, length = 16)
    private String sourceLanguage;

    @Column(name = "target_language", nullable = false, length = 16)
    private String targetLanguage;

    @Column(name = "domain", length = 100)
    private String domain;

    @Lob
    @Column(name = "source_text", columnDefinition = "LONGTEXT")
    private String sourceText;

    @Column(name = "input_file_id")
    private Long inputFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private AgentTaskStatusEnum status = AgentTaskStatusEnum.PENDING;

    @Column(name = "current_step", length = 64)
    private String currentStep;

    @Column(name = "selected_model", length = 64)
    private String selectedModel;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "need_human_review", nullable = false)
    @Builder.Default
    private Boolean needHumanReview = false;

    @Column(name = "final_quality_score")
    private Integer finalQualityScore;

    @Lob
    @Column(name = "final_response", columnDefinition = "LONGTEXT")
    private String finalResponse;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
