package cn.net.wanzni.ai.translation.service.impl.agent;

import cn.net.wanzni.ai.translation.dto.QualityAssessmentResponse;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.entity.TranslationMemoryEntry;
import cn.net.wanzni.ai.translation.enums.AgentStepTypeEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.repository.AgentTaskRepository;
import cn.net.wanzni.ai.translation.repository.AgentTaskStepRepository;
import cn.net.wanzni.ai.translation.repository.ReviewTaskRepository;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.QualityAssessmentService;
import cn.net.wanzni.ai.translation.service.ReviewTaskService;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import cn.net.wanzni.ai.translation.service.TranslationService;
import cn.net.wanzni.ai.translation.service.llm.RagService;
import cn.net.wanzni.ai.translation.service.sse.AgentTaskEventStreamService;
import cn.net.wanzni.ai.translation.service.translation.QwenTranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.*;

class AgentWorkflowServiceImplTest {

    @Test
    void shouldCreateReviewTaskDuringFinalizeWhenHumanReviewIsNeeded() {
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        AgentTaskStepRepository agentTaskStepRepository = mock(AgentTaskStepRepository.class);
        RagService ragService = mock(RagService.class);
        TranslationService translationService = mock(TranslationService.class);
        QwenTranslationService qwenTranslationService = mock(QwenTranslationService.class);
        TranslationMemoryService translationMemoryService = mock(TranslationMemoryService.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        QualityAssessmentService qualityAssessmentService = mock(QualityAssessmentService.class);
        AgentTaskEventStreamService eventStreamService = mock(AgentTaskEventStreamService.class);
        Executor executor = Runnable::run;

        AgentWorkflowServiceImpl service = new AgentWorkflowServiceImpl(
                agentTaskRepository,
                agentTaskStepRepository,
                ragService,
                translationService,
                qwenTranslationService,
                translationMemoryService,
                translationRecordRepository,
                reviewTaskRepository,
                reviewTaskService,
                qualityAssessmentService,
                eventStreamService,
                new ObjectMapper(),
                executor
        );

        AgentTask task = AgentTask.builder()
                .id(1L)
                .status(AgentTaskStatusEnum.RUNNING)
                .bizType("TEXT_TRANSLATION")
                .needHumanReview(false)
                .build();
        TranslationResponse translationResponse = TranslationResponse.builder()
                .translationId(11L)
                .translatedText("machine text")
                .translationEngine("QWEN")
                .build();
        QualityAssessmentResponse qualityResponse = QualityAssessmentResponse.builder()
                .overallScore(90)
                .qualityLevel("good")
                .needsHumanReview(true)
                .sensitiveContentDetected(true)
                .tmRejectReasons(List.of("SENSITIVE_CONTENT"))
                .build();
        TranslationRecord record = TranslationRecord.builder()
                .id(11L)
                .agentTaskId(1L)
                .reviewStatus(null)
                .build();
        ReviewTask reviewTask = ReviewTask.builder()
                .id(101L)
                .agentTaskId(1L)
                .reasonCode("SENSITIVE_CONTENT")
                .reviewStatus(ReviewStatusEnum.PENDING)
                .build();

        when(agentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(agentTaskStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewTaskRepository.findFirstByAgentTaskIdAndReviewStatus(1L, ReviewStatusEnum.PENDING)).thenReturn(Optional.empty());
        when(reviewTaskService.createPendingReviewTask(eq(1L), eq("TEXT_TRANSLATION"), eq("11"), eq("SENSITIVE_CONTENT"), any(), eq("machine text")))
                .thenReturn(reviewTask);
        when(translationRecordRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(record));

        service.finalizeTaskSuccess(1L, translationResponse, qualityResponse, false);

        assertTrue(Boolean.TRUE.equals(task.getNeedHumanReview()));
        assertEquals("PENDING", record.getReviewStatus());
        verify(reviewTaskService).createPendingReviewTask(eq(1L), eq("TEXT_TRANSLATION"), eq("11"), eq("SENSITIVE_CONTENT"), any(), eq("machine text"));
        verify(translationRecordRepository).save(record);

        var captor = org.mockito.ArgumentCaptor.forClass(AgentTaskStep.class);
        verify(agentTaskStepRepository).save(captor.capture());
        AgentTaskStep step = captor.getValue();
        assertTrue(step.getOutputJson().contains("\"reviewTaskCreated\":true"));
        assertTrue(step.getOutputJson().contains("\"reviewReasonCode\":\"SENSITIVE_CONTENT\""));

        var inOrder = inOrder(eventStreamService);
        inOrder.verify(eventStreamService).publishStepUpdate(any(AgentTaskStep.class));
        inOrder.verify(eventStreamService).publishTaskUpdate(task, reviewTask);
        inOrder.verify(eventStreamService).publishResult(eq(task), eq(reviewTask), any(), any(), any());
        inOrder.verify(eventStreamService).publishDone(1L, AgentTaskStatusEnum.COMPLETED);
        inOrder.verify(eventStreamService).completeTask(1L);
    }

    @Test
    void shouldExecuteReviseLoopAndFinalizeWithRevisedResult() throws Exception {
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        AgentTaskStepRepository agentTaskStepRepository = mock(AgentTaskStepRepository.class);
        RagService ragService = mock(RagService.class);
        TranslationService translationService = mock(TranslationService.class);
        QwenTranslationService qwenTranslationService = mock(QwenTranslationService.class);
        TranslationMemoryService translationMemoryService = mock(TranslationMemoryService.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        ReviewTaskService reviewTaskService = mock(ReviewTaskService.class);
        QualityAssessmentService qualityAssessmentService = mock(QualityAssessmentService.class);
        AgentTaskEventStreamService eventStreamService = mock(AgentTaskEventStreamService.class);
        Executor executor = Runnable::run;

        AgentWorkflowServiceImpl service = new AgentWorkflowServiceImpl(
                agentTaskRepository,
                agentTaskStepRepository,
                ragService,
                translationService,
                qwenTranslationService,
                translationMemoryService,
                translationRecordRepository,
                reviewTaskRepository,
                reviewTaskService,
                qualityAssessmentService,
                eventStreamService,
                new ObjectMapper(),
                executor
        );

        AgentTask task = AgentTask.builder()
                .id(1L)
                .userId(99L)
                .status(AgentTaskStatusEnum.PENDING)
                .bizType("TEXT_TRANSLATION")
                .sourceText("source text")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .domain("general")
                .selectedModel("QWEN")
                .needHumanReview(false)
                .build();
        TranslationResponse translationResponse = TranslationResponse.builder()
                .translationId(11L)
                .translatedText("draft text")
                .translationEngine("QWEN")
                .status("COMPLETED")
                .build();
        QualityAssessmentResponse initialQuality = QualityAssessmentResponse.builder()
                .overallScore(80)
                .numberScore(90)
                .terminologyScore(100)
                .formatScore(100)
                .needsRetry(true)
                .needsHumanReview(false)
                .hardRulePassed(false)
                .sensitiveContentDetected(false)
                .qualityLevel("NEEDS_FIX")
                .tmRejectReasons(List.of("NUMBER_MISMATCH"))
                .assessmentEngine("QUALITY_ENGINE")
                .build();
        QualityAssessmentResponse postReviseQuality = QualityAssessmentResponse.builder()
                .overallScore(95)
                .numberScore(100)
                .terminologyScore(100)
                .formatScore(100)
                .needsRetry(false)
                .needsHumanReview(false)
                .hardRulePassed(true)
                .sensitiveContentDetected(false)
                .tmEligible(true)
                .qualityLevel("GOOD")
                .tmRejectReasons(List.of())
                .assessmentEngine("QUALITY_ENGINE")
                .build();
        TranslationRecord record = TranslationRecord.builder()
                .id(11L)
                .agentTaskId(1L)
                .translatedText("draft text")
                .build();

        when(agentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(agentTaskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentTaskStepRepository.countByTaskId(1L)).thenReturn(0L, 1L, 2L, 3L, 4L, 5L, 6L);
        when(agentTaskStepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ragService.buildRagContext(any())).thenReturn(cn.net.wanzni.ai.translation.dto.RagContext.builder()
                .glossaryMap(java.util.Map.of())
                .historySnippets(List.of())
                .contextSnippets(List.of())
                .keywords(List.of())
                .build());
        when(translationService.translate(any())).thenReturn(translationResponse);
        when(qualityAssessmentService.assess(any())).thenReturn(initialQuality, postReviseQuality);
        when(qwenTranslationService.complete(any(), any())).thenReturn("revised text");
        when(translationMemoryService.saveApprovedPair(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(mock(TranslationMemoryEntry.class)));
        when(reviewTaskRepository.findFirstByAgentTaskIdAndReviewStatus(1L, ReviewStatusEnum.PENDING)).thenReturn(Optional.empty());
        when(translationRecordRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(record));

        service.executeTextTask(1L);

        assertEquals("revised text", task.getFinalResponse());
        assertEquals("revised text", record.getTranslatedText());
        assertFalse(Boolean.TRUE.equals(task.getNeedHumanReview()));

        var stepCaptor = org.mockito.ArgumentCaptor.forClass(AgentTaskStep.class);
        verify(agentTaskStepRepository, atLeast(7)).save(stepCaptor.capture());
        List<AgentTaskStep> steps = stepCaptor.getAllValues();
        assertTrue(steps.stream().anyMatch(step -> step.getStepType() == AgentStepTypeEnum.REVISE));
        assertEquals(2L, steps.stream().filter(step -> step.getStepType() == AgentStepTypeEnum.QUALITY_CHECK).count());

        verify(eventStreamService).publishResult(eq(task), isNull(), eq("draft text"), eq(true), contains("NUMBER"));
        verify(reviewTaskService, never()).createPendingReviewTask(any(), any(), any(), any(), any(), any());
    }
}
