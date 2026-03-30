package cn.net.wanzni.ai.translation.dto.agent;

import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskResultEvent {

    private Long taskId;

    private String finalResponse;

    private String draftResponse;

    private Integer finalQualityScore;

    private Boolean revisionApplied;

    private String revisionSummary;

    private Boolean needHumanReview;

    private Long reviewTaskId;

    private ReviewStatusEnum reviewStatus;

    private String reviewReasonCode;

    private String selectedModel;
}
