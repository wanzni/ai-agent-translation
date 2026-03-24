package cn.net.wanzni.ai.translation.service.impl.agent;

import cn.net.wanzni.ai.translation.dto.agent.AgentTaskTimelineResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.enums.AgentStepStatusEnum;
import cn.net.wanzni.ai.translation.enums.AgentStepTypeEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.repository.AgentTaskRepository;
import cn.net.wanzni.ai.translation.repository.AgentTaskStepRepository;
import cn.net.wanzni.ai.translation.repository.ReviewTaskRepository;
import cn.net.wanzni.ai.translation.service.agent.AgentWorkflowService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTaskServiceImplTest {

    @Test
    void shouldPersistRequestedTranslationEngineWhenCreatingTask() {
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        AgentTaskStepRepository agentTaskStepRepository = mock(AgentTaskStepRepository.class);
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentWorkflowService agentWorkflowService = mock(AgentWorkflowService.class);

        AgentTaskServiceImpl service = new AgentTaskServiceImpl(
                agentTaskRepository,
                agentTaskStepRepository,
                reviewTaskRepository,
                agentWorkflowService
        );

        when(agentTaskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> {
            AgentTask task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });
        when(agentTaskStepRepository.save(any(AgentTaskStep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentTaskCreateRequest request = AgentTaskCreateRequest.builder()
                .sourceText("hello")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .translationEngine("qwen")
                .build();

        var response = service.createTextTask(request);

        assertEquals("QWEN", response.getSelectedModel());
        verify(agentWorkflowService).submitTextTask(1L);
    }

    @Test
    void shouldReturnTaskTimelineWithReviewInfo() {
        AgentTaskRepository agentTaskRepository = mock(AgentTaskRepository.class);
        AgentTaskStepRepository agentTaskStepRepository = mock(AgentTaskStepRepository.class);
        ReviewTaskRepository reviewTaskRepository = mock(ReviewTaskRepository.class);
        AgentWorkflowService agentWorkflowService = mock(AgentWorkflowService.class);

        AgentTaskServiceImpl service = new AgentTaskServiceImpl(
                agentTaskRepository,
                agentTaskStepRepository,
                reviewTaskRepository,
                agentWorkflowService
        );

        AgentTask task = AgentTask.builder()
                .id(1L)
                .taskNo("AT-001")
                .traceId("trace-001")
                .status(AgentTaskStatusEnum.COMPLETED)
                .currentStep(AgentStepTypeEnum.FINALIZE.name())
                .needHumanReview(false)
                .build();
        AgentTaskStep step = AgentTaskStep.builder()
                .id(10L)
                .taskId(1L)
                .stepNo(1)
                .stepType(AgentStepTypeEnum.CREATED)
                .stepName("Task created")
                .status(AgentStepStatusEnum.SUCCESS)
                .build();
        ReviewTask reviewTask = ReviewTask.builder()
                .id(100L)
                .agentTaskId(1L)
                .reasonCode("SENSITIVE_CONTENT")
                .reviewStatus(ReviewStatusEnum.APPROVED)
                .build();

        when(agentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(agentTaskStepRepository.findByTaskIdOrderByStepNoAsc(1L)).thenReturn(List.of(step));
        when(reviewTaskRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(reviewTask));

        AgentTaskTimelineResponse timeline = service.getTaskTimeline(1L);

        assertEquals(1L, timeline.getTaskId());
        assertEquals(100L, timeline.getReviewTaskId());
        assertEquals(ReviewStatusEnum.APPROVED, timeline.getReviewStatus());
        assertEquals("SENSITIVE_CONTENT", timeline.getReviewReasonCode());
        assertNotNull(timeline.getSteps());
        assertEquals(1, timeline.getSteps().size());
    }
}
