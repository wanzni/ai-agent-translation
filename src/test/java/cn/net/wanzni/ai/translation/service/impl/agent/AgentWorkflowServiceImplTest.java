package cn.net.wanzni.ai.translation.service.impl.agent;

import cn.net.wanzni.ai.translation.dto.QualityAssessmentResponse;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.*;

class AgentWorkflowServiceImplTest {

    @Test
    void shouldCreateReviewTaskDuringFinalizeWhenHumanReviewIsNeeded() {
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        AgentTaskStepRepository agentTaskStepRepository = mock(AgentTaskStepRepository.class);
        RagService ragService = mock(RagService.class);
        TranslationService translationService = mock(TranslationService.class);
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
        inOrder.verify(eventStreamService).publishResult(task, reviewTask);
        inOrder.verify(eventStreamService).publishDone(1L, AgentTaskStatusEnum.COMPLETED);
        inOrder.verify(eventStreamService).completeTask(1L);
    }
}
