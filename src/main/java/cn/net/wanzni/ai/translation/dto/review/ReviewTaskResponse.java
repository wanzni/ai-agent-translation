package cn.net.wanzni.ai.translation.dto.review;

import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewTaskResponse {

    private Long id;
    private Long agentTaskId;
    private String bizType;
    private String bizId;
    private String reasonCode;
    private ReviewStatusEnum reviewStatus;
    private Long reviewerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
