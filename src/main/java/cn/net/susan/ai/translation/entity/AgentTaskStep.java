package cn.net.susan.ai.translation.entity;

import cn.net.susan.ai.translation.enums.AgentStepStatusEnum;
import cn.net.susan.ai.translation.enums.AgentStepTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_task_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 64)
    private AgentStepTypeEnum stepType;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "tool_name", length = 64)
    private String toolName;

    @Column(name = "model_name", length = 64)
    private String modelName;

    @Lob
    @Column(name = "input_json", columnDefinition = "LONGTEXT")
    private String inputJson;

    @Lob
    @Column(name = "output_json", columnDefinition = "LONGTEXT")
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AgentStepStatusEnum status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
