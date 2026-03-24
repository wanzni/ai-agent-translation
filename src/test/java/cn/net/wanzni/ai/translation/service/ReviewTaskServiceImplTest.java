package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.dto.review.ReviewTaskDetailResponse;
import cn.net.wanzni.ai.translation.dto.review.ReviewTaskResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.exception.ResourceNotFoundException;
import cn.net.wanzni.ai.translation.repository.AgentTaskRepository;
import cn.net.wanzni.ai.translation.repository.ReviewTaskRepository;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.impl.ReviewTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewTaskServiceImplTest {

    @Test
    void shouldClaimPendingReviewTaskWhenUnclaimed() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, null);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        ReviewTaskResponse response = service.claim(1L, 99L);

        assertEquals(Long.valueOf(99L), response.getReviewerId());
        assertEquals(Long.valueOf(99L), reviewTask.getReviewerId());
        verify(reviewTaskRepository).save(reviewTask);
    }

    @Test
    void shouldReturnClaimedTaskWhenClaimIsIdempotent() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, 99L);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        ReviewTaskResponse response = service.claim(1L, 99L);

        assertEquals(Long.valueOf(99L), response.getReviewerId());
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectClaimWhenTaskClaimedByAnotherReviewer() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, 77L);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.claim(1L, 99L));

        assertTrue(exception.getMessage().contains("another reviewer"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectClaimWhenTaskAlreadyProcessed() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = ReviewTask.builder()
                .id(1L)
                .agentTaskId(10L)
                .reviewStatus(ReviewStatusEnum.APPROVED)
                .reviewerId(99L)
                .build();
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.claim(1L, 99L));

        assertTrue(exception.getMessage().contains("already processed"));
    }

    @Test
    void shouldReleaseClaimedPendingReviewTask() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, 99L);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        ReviewTaskResponse response = service.release(1L, 99L);

        assertEquals(null, response.getReviewerId());
        assertEquals(null, reviewTask.getReviewerId());
        verify(reviewTaskRepository).save(reviewTask);
    }

    @Test
    void shouldRejectReleaseWhenTaskIsNotClaimed() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, null);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.release(1L, 99L));

        assertTrue(exception.getMessage().contains("not claimed"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectReleaseWhenTaskClaimedByAnotherReviewer() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, 77L);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.release(1L, 99L));

        assertTrue(exception.getMessage().contains("another reviewer"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectReleaseWhenTaskAlreadyProcessed() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = ReviewTask.builder()
                .id(1L)
                .agentTaskId(10L)
                .reviewStatus(ReviewStatusEnum.REVISED)
                .reviewerId(99L)
                .build();
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.release(1L, 99L));

        assertTrue(exception.getMessage().contains("already processed"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldApprovePendingReviewTaskAndSyncAgentAndRecord() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, 99L);
        reviewTask.setBizId("100");
        reviewTask.setSuggestedText("machine text");
        AgentTask agentTask = AgentTask.builder()
                .id(10L)
                .status(AgentTaskStatusEnum.COMPLETED)
                .needHumanReview(true)
                .build();
        TranslationRecord translationRecord = TranslationRecord.builder()
                .id(100L)
                .agentTaskId(10L)
                .translatedText("machine text")
                .reviewStatus("PENDING")
                .build();

        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));
        when(agentTaskRepository.findById(10L)).thenReturn(Optional.of(agentTask));
        when(translationRecordRepository.findById(100L)).thenReturn(Optional.of(translationRecord));

        ReviewTaskResponse response = service.approve(1L, 99L);

        assertEquals(ReviewStatusEnum.APPROVED, response.getReviewStatus());
        assertEquals("machine text", reviewTask.getFinalText());
        assertEquals(Long.valueOf(99L), reviewTask.getReviewerId());
        assertEquals("APPROVED", translationRecord.getReviewStatus());
        assertFalse(Boolean.TRUE.equals(agentTask.getNeedHumanReview()));
        assertEquals("machine text", agentTask.getFinalResponse());
        verify(reviewTaskRepository).save(reviewTask);
        verify(translationRecordRepository).save(translationRecord);
        verify(agentTaskRepository).save(agentTask);
    }

    @Test
    void shouldRejectApproveWhenTaskHasNotBeenClaimed() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, null);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.approve(1L, 99L));

        assertTrue(exception.getMessage().contains("must be claimed"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectApproveWhenTaskClaimedByAnotherReviewer() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(1L, 10L, 77L);
        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.approve(1L, 99L));

        assertTrue(exception.getMessage().contains("another reviewer"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRevisePendingReviewTaskAndOverwriteTranslationRecord() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(2L, 20L, 88L);
        reviewTask.setBizId("200");
        reviewTask.setSuggestedText("old text");
        AgentTask agentTask = AgentTask.builder()
                .id(20L)
                .needHumanReview(true)
                .build();
        TranslationRecord translationRecord = TranslationRecord.builder()
                .id(200L)
                .agentTaskId(20L)
                .translatedText("old text")
                .reviewStatus("PENDING")
                .build();

        when(reviewTaskRepository.findById(2L)).thenReturn(Optional.of(reviewTask));
        when(agentTaskRepository.findById(20L)).thenReturn(Optional.of(agentTask));
        when(translationRecordRepository.findById(200L)).thenReturn(Optional.of(translationRecord));

        ReviewTaskResponse response = service.revise(2L, "human revised text", 88L);

        assertEquals(ReviewStatusEnum.REVISED, response.getReviewStatus());
        assertEquals("human revised text", reviewTask.getFinalText());
        assertEquals("human revised text", translationRecord.getTranslatedText());
        assertEquals("REVISED", translationRecord.getReviewStatus());
        assertFalse(Boolean.TRUE.equals(agentTask.getNeedHumanReview()));
        assertEquals("human revised text", agentTask.getFinalResponse());
    }

    @Test
    void shouldRejectReviseWhenTaskHasNotBeenClaimed() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(2L, 20L, null);
        when(reviewTaskRepository.findById(2L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.revise(2L, "human revised text", 88L));

        assertTrue(exception.getMessage().contains("must be claimed"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectReviseWhenTaskClaimedByAnotherReviewer() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(2L, 20L, 77L);
        when(reviewTaskRepository.findById(2L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.revise(2L, "human revised text", 88L));

        assertTrue(exception.getMessage().contains("another reviewer"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldRejectProcessingWhenReviewTaskAlreadyHandled() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = ReviewTask.builder()
                .id(3L)
                .agentTaskId(30L)
                .reviewStatus(ReviewStatusEnum.APPROVED)
                .build();
        when(reviewTaskRepository.findById(3L)).thenReturn(Optional.of(reviewTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.approve(3L, 7L));

        assertTrue(exception.getMessage().contains("already processed"));
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldCreatePendingReviewTaskOnlyOncePerAgentTask() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask existing = ReviewTask.builder()
                .id(4L)
                .agentTaskId(40L)
                .reviewStatus(ReviewStatusEnum.PENDING)
                .build();
        when(reviewTaskRepository.findFirstByAgentTaskIdAndReviewStatus(40L, ReviewStatusEnum.PENDING))
                .thenReturn(Optional.of(existing));

        ReviewTask result = service.createPendingReviewTask(40L, "TEXT_TRANSLATION", "400", "SENSITIVE_CONTENT", "{}", "draft");

        assertSame(existing, result);
        verify(reviewTaskRepository, never()).save(any());
    }

    @Test
    void shouldReturnDetailWithRecordAndTaskContext() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = ReviewTask.builder()
                .id(5L)
                .agentTaskId(50L)
                .bizId("500")
                .reasonCode("SENSITIVE_CONTENT")
                .issueSummary("{\"overallScore\":95}")
                .suggestedText("machine")
                .reviewStatus(ReviewStatusEnum.PENDING)
                .build();
        AgentTask agentTask = AgentTask.builder()
                .id(50L)
                .status(AgentTaskStatusEnum.COMPLETED)
                .sourceText("source")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build();
        TranslationRecord record = TranslationRecord.builder()
                .id(500L)
                .agentTaskId(50L)
                .sourceText("source")
                .translatedText("machine")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .build();

        when(reviewTaskRepository.findById(5L)).thenReturn(Optional.of(reviewTask));
        when(agentTaskRepository.findById(50L)).thenReturn(Optional.of(agentTask));
        when(translationRecordRepository.findById(500L)).thenReturn(Optional.of(record));

        ReviewTaskDetailResponse response = service.get(5L);

        assertEquals(Long.valueOf(500L), response.getTranslationRecordId());
        assertEquals("source", response.getSourceText());
        assertEquals("machine", response.getTranslatedText());
        assertEquals(AgentTaskStatusEnum.COMPLETED, response.getAgentTaskStatus());
    }

    @Test
    void shouldListReviewTasksByStatusReasonCodeAndReviewer() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(6L, 60L, 99L);
        reviewTask.setReasonCode("SENSITIVE_CONTENT");
        when(reviewTaskRepository.findByReviewStatusAndReasonCodeAndReviewerIdOrderByCreatedAtDesc(
                eq(ReviewStatusEnum.PENDING), eq("SENSITIVE_CONTENT"), eq(99L), any()))
                .thenReturn(new PageImpl<>(List.of(reviewTask)));

        var page = service.list(null, "SENSITIVE_CONTENT", 99L, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(Long.valueOf(6L), page.getContent().get(0).getId());
    }

    @Test
    void shouldListReviewTasksByStatusOnlyWhenNoFiltersProvided() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(7L, 70L, null);
        when(reviewTaskRepository.findByReviewStatusOrderByCreatedAtDesc(eq(ReviewStatusEnum.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(reviewTask)));

        var page = service.list(null, null, null, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(Long.valueOf(7L), page.getContent().get(0).getId());
    }

    @Test
    void shouldThrowWhenReviewTaskNotFound() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        when(reviewTaskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(999L));
    }

    @Test
    void shouldSaveHumanReviewedPairToTranslationMemoryWhenApproved() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        ReviewTask reviewTask = pendingTask(8L, 80L, 99L);
        reviewTask.setBizId("800");
        reviewTask.setSuggestedText("machine approved");
        AgentTask agentTask = AgentTask.builder()
                .id(80L)
                .userId(11L)
                .domain("saas")
                .needHumanReview(true)
                .build();
        TranslationRecord translationRecord = TranslationRecord.builder()
                .id(800L)
                .agentTaskId(80L)
                .sourceText("source text")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .qualityScore(91)
                .translatedText("machine approved")
                .reviewStatus("PENDING")
                .build();

        when(reviewTaskRepository.findById(8L)).thenReturn(Optional.of(reviewTask));
        when(agentTaskRepository.findById(80L)).thenReturn(Optional.of(agentTask));
        when(translationRecordRepository.findById(800L)).thenReturn(Optional.of(translationRecord));
        when(translationMemoryService.saveApprovedPair(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        service.approve(8L, 99L);

        verify(translationMemoryService).saveApprovedPair(
                "source text",
                "machine approved",
                "en",
                "zh",
                "saas",
                91,
                true,
                false,
                true,
                true,
                80L,
                11L
        );
    }

    @Test
    void shouldReturnReviewQueueStats() {
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        cn.net.wanzni.ai.translation.service.TranslationMemoryService translationMemoryService = mock(cn.net.wanzni.ai.translation.service.TranslationMemoryService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskRepository, agentTaskRepository, translationRecordRepository, translationMemoryService);

        when(reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.PENDING)).thenReturn(5L);
        when(reviewTaskRepository.countByReviewStatusAndReviewerIdIsNull(ReviewStatusEnum.PENDING)).thenReturn(2L);
        when(reviewTaskRepository.countByReviewStatusAndReviewerIdIsNotNull(ReviewStatusEnum.PENDING)).thenReturn(3L);
        when(reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.APPROVED)).thenReturn(4L);
        when(reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.REVISED)).thenReturn(1L);
        when(reviewTaskRepository.countByReviewStatus(ReviewStatusEnum.REJECTED)).thenReturn(0L);
        when(reviewTaskRepository.countByReasonCode()).thenReturn(java.util.Collections.singletonList(new Object[]{"SENSITIVE_CONTENT", 3L}));
        when(reviewTaskRepository.countByReviewerId()).thenReturn(java.util.Collections.singletonList(new Object[]{99L, 2L}));

        var stats = service.stats();

        assertEquals(5L, stats.getPendingCount());
        assertEquals(2L, stats.getPublicQueueCount());
        assertEquals(3L, stats.getClaimedPendingCount());
        assertEquals(5L, stats.getProcessedCount());
        assertEquals("SENSITIVE_CONTENT", stats.getReasonCounts().get(0).getReasonCode());
        assertEquals(99L, stats.getReviewerCounts().get(0).getReviewerId());
    }

    private ReviewTask pendingTask(Long reviewTaskId, Long agentTaskId, Long reviewerId) {
        return ReviewTask.builder()
                .id(reviewTaskId)
                .agentTaskId(agentTaskId)
                .reviewStatus(ReviewStatusEnum.PENDING)
                .reviewerId(reviewerId)
                .build();
    }
}
