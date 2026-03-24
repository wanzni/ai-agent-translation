package cn.net.wanzni.ai.translation.dto.agent;

import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskTimelineResponse {

    private Long taskId;

    private String taskNo;

    private String traceId;

    private AgentTaskStatusEnum taskStatus;

    private String currentStep;

    private Boolean needHumanReview;

    private ReviewStatusEnum reviewStatus;

    private Long reviewTaskId;

    private String reviewReasonCode;

    private List<AgentTaskStepResponse> steps;
}
