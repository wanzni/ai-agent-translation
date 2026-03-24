package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.dto.review.ReviewTaskDetailResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewQueueStatsResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewTaskResponse;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewTaskService {

    Page<ReviewTaskResponse> list(ReviewStatusEnum status, String reasonCode, Long reviewerId, Pageable pageable);

    Page<ReviewTaskResponse> listPublicQueue(String reasonCode, Pageable pageable);

    Page<ReviewTaskResponse> listMyTasks(Long reviewerId, Pageable pageable);

    Page<ReviewTaskResponse> listProcessedTasks(String reasonCode, Pageable pageable);

    ReviewQueueStatsResponse stats();

    ReviewTaskDetailResponse get(Long reviewTaskId);

    ReviewTaskResponse claim(Long reviewTaskId, Long reviewerId);

    ReviewTaskResponse release(Long reviewTaskId, Long reviewerId);

    ReviewTaskResponse approve(Long reviewTaskId, Long reviewerId);

    ReviewTaskResponse revise(Long reviewTaskId, String finalText, Long reviewerId);

    ReviewTask createPendingReviewTask(Long agentTaskId,
                                       String bizType,
                                       String bizId,
                                       String reasonCode,
                                       String issueSummary,
                                       String suggestedText);
}
