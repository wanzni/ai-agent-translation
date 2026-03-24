package cn.net.wanzni.ai.translation.dto.agent;

import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskStatusEvent {

    private Long taskId;

    private AgentTaskStatusEnum taskStatus;

    private String currentStep;

    private Boolean needHumanReview;

    private Long reviewTaskId;

    private ReviewStatusEnum reviewStatus;

    private String reviewReasonCode;

    private Integer finalQualityScore;

    private String errorMessage;

    private String selectedModel;
}
