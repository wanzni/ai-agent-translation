package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskTimelineResponse;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.service.agent.AgentTaskService;
import cn.net.wanzni.ai.translation.service.sse.AgentTaskEventStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTaskControllerTest {

    @Test
    void shouldSendConnectedAndSnapshotWhenSubscribingRunningTask() {
        AgentTaskService agentTaskService = mock(AgentTaskService.class);
        AgentTaskEventStreamService eventStreamService = mock(AgentTaskEventStreamService.class);
        AgentTaskController controller = new AgentTaskController(agentTaskService, eventStreamService);

        AgentTaskResponse task = AgentTaskResponse.builder()
                .id(1L)
                .taskNo("AT-001")
                .traceId("trace-001")
                .status(AgentTaskStatusEnum.RUNNING)
                .build();
        AgentTaskTimelineResponse timeline = AgentTaskTimelineResponse.builder()
                .taskId(1L)
                .build();
        SseEmitter emitter = new SseEmitter();

        when(agentTaskService.getTask(1L)).thenReturn(task);
        when(agentTaskService.getTaskTimeline(1L)).thenReturn(timeline);
        when(eventStreamService.register(1L)).thenReturn(emitter);

        SseEmitter response = controller.subscribeTaskEvents(1L);

        assertNotNull(response);
        var inOrder = inOrder(agentTaskService, eventStreamService);
        inOrder.verify(agentTaskService).getTask(1L);
        inOrder.verify(agentTaskService).getTaskTimeline(1L);
        inOrder.verify(eventStreamService).register(1L);
        inOrder.verify(eventStreamService).sendConnected(emitter, task);
        inOrder.verify(eventStreamService).sendSnapshot(emitter, timeline);
        verify(eventStreamService, never()).sendResult(emitter, task, timeline);
        verify(eventStreamService, never()).sendDone(emitter, 1L, AgentTaskStatusEnum.RUNNING);
    }

    @Test
    void shouldSendTerminalEventsAndCleanupWhenSubscribingCompletedTask() {
        AgentTaskService agentTaskService = mock(AgentTaskService.class);
        AgentTaskEventStreamService eventStreamService = mock(AgentTaskEventStreamService.class);
        AgentTaskController controller = new AgentTaskController(agentTaskService, eventStreamService);

        AgentTaskResponse task = AgentTaskResponse.builder()
                .id(2L)
                .taskNo("AT-002")
                .traceId("trace-002")
                .status(AgentTaskStatusEnum.COMPLETED)
                .finalResponse("translated")
                .build();
        AgentTaskTimelineResponse timeline = AgentTaskTimelineResponse.builder()
                .taskId(2L)
                .build();
        SseEmitter emitter = new SseEmitter();

        when(agentTaskService.getTask(2L)).thenReturn(task);
        when(agentTaskService.getTaskTimeline(2L)).thenReturn(timeline);
        when(eventStreamService.register(2L)).thenReturn(emitter);

        SseEmitter response = controller.subscribeTaskEvents(2L);

        assertNotNull(response);
        var inOrder = inOrder(eventStreamService);
        inOrder.verify(eventStreamService).register(2L);
        inOrder.verify(eventStreamService).sendConnected(emitter, task);
        inOrder.verify(eventStreamService).sendSnapshot(emitter, timeline);
        inOrder.verify(eventStreamService).sendResult(emitter, task, timeline);
        inOrder.verify(eventStreamService).sendDone(emitter, 2L, AgentTaskStatusEnum.COMPLETED);
        inOrder.verify(eventStreamService).unregister(2L, emitter);
    }
}
