package cn.net.susan.ai.translation.dto.agent;

import cn.net.susan.ai.translation.enums.AgentStepStatusEnum;
import cn.net.susan.ai.translation.enums.AgentStepTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskStepResponse {

    private Long id;
    private Long taskId;
    private Integer stepNo;
    private AgentStepTypeEnum stepType;
    private String stepName;
    private String toolName;
    private String modelName;
    private String inputJson;
    private String outputJson;
    private AgentStepStatusEnum status;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
