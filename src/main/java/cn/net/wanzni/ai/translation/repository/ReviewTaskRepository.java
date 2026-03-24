package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {

    Page<ReviewTask> findByReviewStatusOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus, Pageable pageable);

    Page<ReviewTask> findByReviewStatusAndReasonCodeOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus,
                                                                         String reasonCode,
                                                                         Pageable pageable);

    Page<ReviewTask> findByReviewStatusAndReviewerIdOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus,
                                                                         Long reviewerId,
                                                                         Pageable pageable);

    Page<ReviewTask> findByReviewStatusAndReasonCodeAndReviewerIdOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus,
                                                                                       String reasonCode,
                                                                                       Long reviewerId,
                                                                                       Pageable pageable);

    Page<ReviewTask> findByReviewStatusAndReviewerIdIsNullOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus,
                                                                               Pageable pageable);

    Page<ReviewTask> findByReviewStatusAndReviewerIdIsNullAndReasonCodeOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus,
                                                                                             String reasonCode,
                                                                                             Pageable pageable);

    Page<ReviewTask> findByReviewStatusInOrderByUpdatedAtDesc(java.util.Collection<ReviewStatusEnum> reviewStatuses,
                                                              Pageable pageable);

    Page<ReviewTask> findByReviewStatusInAndReasonCodeOrderByUpdatedAtDesc(java.util.Collection<ReviewStatusEnum> reviewStatuses,
                                                                           String reasonCode,
                                                                           Pageable pageable);

    long countByReviewStatus(ReviewStatusEnum reviewStatus);

    long countByReviewStatusAndReviewerIdIsNull(ReviewStatusEnum reviewStatus);

    long countByReviewStatusAndReviewerIdIsNotNull(ReviewStatusEnum reviewStatus);

    long countByReviewStatusAndReviewerId(ReviewStatusEnum reviewStatus, Long reviewerId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT rt.reasonCode, COUNT(rt)
            FROM ReviewTask rt
            GROUP BY rt.reasonCode
            ORDER BY COUNT(rt) DESC
            """)
    java.util.List<Object[]> countByReasonCode();

    @org.springframework.data.jpa.repository.Query("""
            SELECT rt.reviewerId, COUNT(rt)
            FROM ReviewTask rt
            WHERE rt.reviewerId IS NOT NULL
            GROUP BY rt.reviewerId
            ORDER BY COUNT(rt) DESC
            """)
    java.util.List<Object[]> countByReviewerId();

    Optional<ReviewTask> findFirstByAgentTaskIdAndReviewStatus(Long agentTaskId, ReviewStatusEnum reviewStatus);

    Optional<ReviewTask> findFirstByAgentTaskIdOrderByCreatedAtDesc(Long agentTaskId);
}
