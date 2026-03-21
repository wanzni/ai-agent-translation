package cn.net.wanzni.ai.translation.dto.agent;

import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskResponse {

    private Long id;
    private String taskNo;
    private Long userId;
    private AgentTaskTypeEnum taskType;
    private String bizType;
    private String bizId;
    private String sourceLanguage;
    private String targetLanguage;
    private String domain;
    private String sourceText;
    private AgentTaskStatusEnum status;
    private String currentStep;
    private String selectedModel;
    private Integer retryCount;
    private Boolean needHumanReview;
    private Integer finalQualityScore;
    private String finalResponse;
    private String traceId;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
