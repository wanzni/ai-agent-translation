package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.review.ReviewTaskDetailResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewQueueStatsResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewTaskResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.exception.ResourceNotFoundException;
import cn.net.wanzni.ai.translation.repository.AgentTaskRepository;
import cn.net.wanzni.ai.translation.repository.ReviewTaskRepository;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.ReviewTaskService;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskRepository reviewTaskRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final TranslationRecordRepository translationRecordRepository;
    private final TranslationMemoryService translationMemoryService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewTaskResponse> list(ReviewStatusEnum status,
                                         String reasonCode,
                                         Long reviewerId,
                                         Pageable pageable) {
        ReviewStatusEnum effectiveStatus = status == null ? ReviewStatusEnum.PENDING : status;
        boolean hasReasonCode = StringUtils.hasText(reasonCode);
        boolean hasReviewerId = reviewerId != null;

        if (hasReasonCode && hasReviewerId) {
            return reviewTaskRepository
                    .findByReviewStatusAndReasonCodeAndReviewerIdOrderByCreatedAtDesc(
                            effectiveStatus, reasonCode, reviewerId, pageable)
                    .map(this::toResponse);
        }
        if (hasReasonCode) {
            return reviewTaskRepository
                    .findByReviewStatusAndReasonCodeOrderByCreatedAtDesc(effectiveStatus, reasonCode, pageable)
                    .map(this::toResponse);
        }
        if (hasReviewerId) {
            return reviewTaskRepository
                    .findByReviewStatusAndReviewerIdOrderByCreatedAtDesc(effectiveStatus, reviewerId, pageable)
                    .map(this::toResponse);
        }
        return reviewTaskRepository.findByReviewStatusOrderByCreatedAtDesc(effectiveStatus, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewTaskResponse> listPublicQueue(String reasonCode, Pageable pageable) {
        if (StringUtils.hasText(reasonCode)) {
            return reviewTaskRepository.findByReviewStatusAndReviewerIdIsNullAndReasonCodeOrderByCreatedAtDesc(
                    ReviewStatusEnum.PENDING, reasonCode, pageable
            ).map(this::toResponse);
        }
        return reviewTaskRepository.findByReviewStatusAndReviewerIdIsNullOrderByCreatedAtDesc(
                ReviewStatusEnum.PENDING, pageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewTaskResponse> listMyTasks(Long reviewerId, Pageable pageable) {
        ensureReviewer(reviewerId);
        return reviewTaskRepository.findByReviewStatusAndReviewerIdOrderByCreatedAtDesc(
                ReviewStatusEnum.PENDING, reviewerId, pageable
        ).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewTaskResponse> listProcessedTasks(String reasonCode, Pageable pageable) {
        List<ReviewStatusEnum> processedStatuses = List.of(
                ReviewStatusEnum.APPROVED,
                ReviewStatusEnum.REVISED,
                ReviewStatusEnum.REJECTED
        );
        if (StringUtils.hasText(reasonCode)) {
            return reviewTaskRepository.findByReviewStatusInAndReasonCodeOrderByUpdatedAtDesc(
                    processedStatuses, reasonCode, pageable
            ).map(this::toResponse);
        }
        return reviewTaskRepository.findByReviewStatusInOrderByUpdatedAtDesc(processedStatuses, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewQueueStatsResponse stats() {
        long pendingCount = reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.PENDING);
        long publicQueueCount = reviewTaskRepository.countByReviewStatusAndReviewerIdIsNull(ReviewStatusEnum.PENDING);
        long claimedPendingCount = reviewTaskRepository.countByReviewStatusAndReviewerIdIsNotNull(ReviewStatusEnum.PENDING);
        long processedCount = reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.APPROVED)
                + reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.REVISED)
                + reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.REJECTED);
        List<ReviewQueueStatsResponse.ReasonCount> reasonCounts = reviewTaskRepository.countByReasonCode().stream()
                .map(row -> ReviewQueueStatsResponse.ReasonCount.builder()
                        .reasonCode(row[0] == null ? null : String.valueOf(row[0]))
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
        List<ReviewQueueStatsResponse.ReviewerCount> reviewerCounts = reviewTaskRepository.countByReviewerId().stream()
                .map(row -> ReviewQueueStatsResponse.ReviewerCount.builder()
                        .reviewerId(row[0] == null ? null : ((Number) row[0]).longValue())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
        return ReviewQueueStatsResponse.builder()
                .pendingCount(pendingCount)
                .publicQueueCount(publicQueueCount)
                .claimedPendingCount(claimedPendingCount)
                .processedCount(processedCount)
                .reasonCounts(reasonCounts)
                .reviewerCounts(reviewerCounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewTaskDetailResponse get(Long reviewTaskId) {
        ReviewTask reviewTask = getRequiredReviewTask(reviewTaskId);
        AgentTask agentTask = getRequiredAgentTask(reviewTask.getAgentTaskId());
        TranslationRecord translationRecord = resolveTranslationRecord(reviewTask).orElse(null);
        return toDetailResponse(reviewTask, agentTask, translationRecord);
    }

    @Override
    @Transactional
    public ReviewTaskResponse claim(Long reviewTaskId, Long reviewerId) {
        ensureReviewer(reviewerId);

        ReviewTask reviewTask = getRequiredReviewTask(reviewTaskId);
        ensurePending(reviewTask);

        if (reviewTask.getReviewerId() == null) {
            reviewTask.setReviewerId(reviewerId);
            reviewTaskRepository.save(reviewTask);
            return toResponse(reviewTask);
        }
        if (reviewerId.equals(reviewTask.getReviewerId())) {
            return toResponse(reviewTask);
        }
        throw new IllegalStateException("Review task is already claimed by another reviewer");
    }

    @Override
    @Transactional
    public ReviewTaskResponse release(Long reviewTaskId, Long reviewerId) {
        ensureReviewer(reviewerId);

        ReviewTask reviewTask = getRequiredReviewTask(reviewTaskId);
        ensurePending(reviewTask);
        if (reviewTask.getReviewerId() == null) {
            throw new IllegalStateException("Review task is not claimed");
        }
        if (!reviewerId.equals(reviewTask.getReviewerId())) {
            throw new IllegalStateException("Review task is claimed by another reviewer");
        }

        reviewTask.setReviewerId(null);
        reviewTaskRepository.save(reviewTask);
        return toResponse(reviewTask);
    }

    @Override
    @Transactional
    public ReviewTaskResponse approve(Long reviewTaskId, Long reviewerId) {
        ensureReviewer(reviewerId);

        ReviewTask reviewTask = getRequiredReviewTask(reviewTaskId);
        ensurePending(reviewTask);
        ensureClaimedByReviewer(reviewTask, reviewerId);
        AgentTask agentTask = getRequiredAgentTask(reviewTask.getAgentTaskId());
        TranslationRecord translationRecord = getRequiredTranslationRecord(reviewTask);

        reviewTask.setReviewStatus(ReviewStatusEnum.APPROVED);
        reviewTask.setReviewerId(reviewerId);
        reviewTask.setFinalText(reviewTask.getSuggestedText());
        reviewTaskRepository.save(reviewTask);

        translationRecord.setReviewStatus(ReviewStatusEnum.APPROVED.name());
        translationRecordRepository.save(translationRecord);

        agentTask.setNeedHumanReview(false);
        agentTask.setFinalResponse(reviewTask.getSuggestedText());
        agentTaskRepository.save(agentTask);

        saveHumanReviewedTranslationMemory(agentTask, translationRecord, reviewTask.getSuggestedText());

        return toResponse(reviewTask);
    }

    @Override
    @Transactional
    public ReviewTaskResponse revise(Long reviewTaskId, String finalText, Long reviewerId) {
        ensureReviewer(reviewerId);
        if (!StringUtils.hasText(finalText)) {
            throw new IllegalArgumentException("finalText cannot be blank");
        }

        ReviewTask reviewTask = getRequiredReviewTask(reviewTaskId);
        ensurePending(reviewTask);
        ensureClaimedByReviewer(reviewTask, reviewerId);
        AgentTask agentTask = getRequiredAgentTask(reviewTask.getAgentTaskId());
        TranslationRecord translationRecord = getRequiredTranslationRecord(reviewTask);

        reviewTask.setReviewStatus(ReviewStatusEnum.REVISED);
        reviewTask.setReviewerId(reviewerId);
        reviewTask.setFinalText(finalText);
        reviewTaskRepository.save(reviewTask);

        translationRecord.setTranslatedText(finalText);
        translationRecord.setReviewStatus(ReviewStatusEnum.REVISED.name());
        translationRecordRepository.save(translationRecord);

        agentTask.setNeedHumanReview(false);
        agentTask.setFinalResponse(finalText);
        agentTaskRepository.save(agentTask);

        saveHumanReviewedTranslationMemory(agentTask, translationRecord, finalText);

        return toResponse(reviewTask);
    }

    @Override
    @Transactional
    public ReviewTask createPendingReviewTask(Long agentTaskId,
                                              String bizType,
                                              String bizId,
                                              String reasonCode,
                                              String issueSummary,
                                              String suggestedText) {
        ReviewTask existing = reviewTaskRepository
                .findFirstByAgentTaskIdAndReviewStatus(agentTaskId, ReviewStatusEnum.PENDING)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        ReviewTask reviewTask = ReviewTask.builder()
                .agentTaskId(agentTaskId)
                .bizType(StringUtils.hasText(bizType) ? bizType : "TEXT_TRANSLATION")
                .bizId(bizId)
                .reasonCode(reasonCode)
                .issueSummary(issueSummary)
                .suggestedText(suggestedText)
                .finalText(null)
                .reviewStatus(ReviewStatusEnum.PENDING)
                .reviewerId(null)
                .build();
        ReviewTask saved = reviewTaskRepository.save(reviewTask);
        log.info("Created review task: id={}, agentTaskId={}, reasonCode={}", saved.getId(), agentTaskId, reasonCode);
        return saved;
    }

    private ReviewTask getRequiredReviewTask(Long reviewTaskId) {
        return reviewTaskRepository.findById(reviewTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Review task not found: " + reviewTaskId));
    }

    private AgentTask getRequiredAgentTask(Long agentTaskId) {
        return agentTaskRepository.findById(agentTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + agentTaskId));
    }

    private TranslationRecord getRequiredTranslationRecord(ReviewTask reviewTask) {
        return resolveTranslationRecord(reviewTask)
                .orElseThrow(() -> new ResourceNotFoundException("Translation record not found for review task: " + reviewTask.getId()));
    }

    private java.util.Optional<TranslationRecord> resolveTranslationRecord(ReviewTask reviewTask) {
        Long translationRecordId = parseTranslationRecordId(reviewTask.getBizId());
        if (translationRecordId != null) {
            java.util.Optional<TranslationRecord> byId = translationRecordRepository.findById(translationRecordId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        return translationRecordRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(reviewTask.getAgentTaskId());
    }

    private Long parseTranslationRecordId(String bizId) {
        if (!StringUtils.hasText(bizId)) {
            return null;
        }
        try {
            return Long.parseLong(bizId);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private void ensureReviewer(Long reviewerId) {
        if (reviewerId == null) {
            throw new IllegalStateException("Reviewer must be logged in");
        }
    }

    private void ensurePending(ReviewTask reviewTask) {
        if (reviewTask.getReviewStatus() != ReviewStatusEnum.PENDING) {
            throw new IllegalStateException("Review task is already processed: " + reviewTask.getId());
        }
    }

    private void ensureClaimedByReviewer(ReviewTask reviewTask, Long reviewerId) {
        if (reviewTask.getReviewerId() == null) {
            throw new IllegalStateException("Review task must be claimed before processing");
        }
        if (!reviewerId.equals(reviewTask.getReviewerId())) {
            throw new IllegalStateException("Review task is claimed by another reviewer");
        }
    }

    private void saveHumanReviewedTranslationMemory(AgentTask agentTask,
                                                    TranslationRecord translationRecord,
                                                    String finalText) {
        try {
            translationMemoryService.saveApprovedPair(
                    translationRecord.getSourceText(),
                    finalText,
                    translationRecord.getSourceLanguage(),
                    translationRecord.getTargetLanguage(),
                    agentTask.getDomain(),
                    translationRecord.getQualityScore(),
                    Boolean.TRUE,
                    Boolean.FALSE,
                    Boolean.TRUE,
                    Boolean.TRUE,
                    agentTask.getId(),
                    agentTask.getUserId()
            );
        } catch (Exception e) {
            log.warn("Save human reviewed translation memory failed for task {}: {}", agentTask.getId(), e.getMessage());
        }
    }

    private ReviewTaskResponse toResponse(ReviewTask reviewTask) {
        return ReviewTaskResponse.builder()
                .id(reviewTask.getId())
                .agentTaskId(reviewTask.getAgentTaskId())
                .bizType(reviewTask.getBizType())
                .bizId(reviewTask.getBizId())
                .reasonCode(reviewTask.getReasonCode())
                .reviewStatus(reviewTask.getReviewStatus())
                .reviewerId(reviewTask.getReviewerId())
                .createdAt(reviewTask.getCreatedAt())
                .updatedAt(reviewTask.getUpdatedAt())
                .build();
    }

    private ReviewTaskDetailResponse toDetailResponse(ReviewTask reviewTask,
                                                      AgentTask agentTask,
                                                      TranslationRecord translationRecord) {
        return ReviewTaskDetailResponse.builder()
                .id(reviewTask.getId())
                .agentTaskId(reviewTask.getAgentTaskId())
                .bizType(reviewTask.getBizType())
                .bizId(reviewTask.getBizId())
                .reasonCode(reviewTask.getReasonCode())
                .reviewStatus(reviewTask.getReviewStatus())
                .reviewerId(reviewTask.getReviewerId())
                .createdAt(reviewTask.getCreatedAt())
                .updatedAt(reviewTask.getUpdatedAt())
                .issueSummary(reviewTask.getIssueSummary())
                .suggestedText(reviewTask.getSuggestedText())
                .finalText(reviewTask.getFinalText())
                .sourceText(translationRecord != null ? translationRecord.getSourceText() : agentTask.getSourceText())
                .translatedText(translationRecord != null ? translationRecord.getTranslatedText() : reviewTask.getSuggestedText())
                .sourceLanguage(translationRecord != null ? translationRecord.getSourceLanguage() : agentTask.getSourceLanguage())
                .targetLanguage(translationRecord != null ? translationRecord.getTargetLanguage() : agentTask.getTargetLanguage())
                .agentTaskStatus(agentTask.getStatus())
                .translationRecordId(translationRecord != null ? translationRecord.getId() : null)
                .build();
    }
}
