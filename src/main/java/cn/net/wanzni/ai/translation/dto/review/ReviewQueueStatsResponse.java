package cn.net.wanzni.ai.translation.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewQueueStatsResponse {

    private Long pendingCount;

    private Long publicQueueCount;

    private Long claimedPendingCount;

    private Long processedCount;

    private List<ReasonCount> reasonCounts;

    private List<ReviewerCount> reviewerCounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReasonCount {
        private String reasonCode;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewerCount {
        private Long reviewerId;
        private Long count;
    }
}
