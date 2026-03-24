package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskStepResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskTimelineResponse;
import cn.net.wanzni.ai.translation.service.agent.AgentTaskService;
import cn.net.wanzni.ai.translation.service.sse.AgentTaskEventStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agent/tasks")
@RequiredArgsConstructor
@Validated
public class AgentTaskController {

    private final AgentTaskService agentTaskService;
    private final AgentTaskEventStreamService agentTaskEventStreamService;

    @PostMapping("/text")
    public AgentTaskResponse createTextTask(@Valid @RequestBody AgentTaskCreateRequest request) {
        log.info("Create text agent task request received: {} -> {}", request.getSourceLanguage(), request.getTargetLanguage());
        return agentTaskService.createTextTask(request);
    }

    @GetMapping("/{taskId}")
    public AgentTaskResponse getTask(@PathVariable Long taskId) {
        log.info("Get agent task detail: {}", taskId);
        return agentTaskService.getTask(taskId);
    }

    @GetMapping("/{taskId}/steps")
    public List<AgentTaskStepResponse> getTaskSteps(@PathVariable Long taskId) {
        log.info("Get agent task steps: {}", taskId);
        return agentTaskService.getTaskSteps(taskId);
    }

    @GetMapping("/{taskId}/timeline")
    public AgentTaskTimelineResponse getTaskTimeline(@PathVariable Long taskId) {
        log.info("Get agent task timeline: {}", taskId);
        return agentTaskService.getTaskTimeline(taskId);
    }

    @GetMapping(value = "/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeTaskEvents(@PathVariable Long taskId) {
        log.info("Subscribe agent task events: {}", taskId);
        AgentTaskResponse task = agentTaskService.getTask(taskId);
        AgentTaskTimelineResponse timeline = agentTaskService.getTaskTimeline(taskId);

        SseEmitter emitter = agentTaskEventStreamService.register(taskId);
        agentTaskEventStreamService.sendConnected(emitter, task);
        agentTaskEventStreamService.sendSnapshot(emitter, timeline);

        if (isTerminal(task.getStatus())) {
            if (task.getFinalResponse() != null) {
                agentTaskEventStreamService.sendResult(emitter, task, timeline);
            }
            agentTaskEventStreamService.sendDone(emitter, taskId, task.getStatus());
            emitter.complete();
            agentTaskEventStreamService.unregister(taskId, emitter);
        }
        return emitter;
    }

    private boolean isTerminal(cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum status) {
        return status == cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum.COMPLETED
                || status == cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum.FAILED
                || status == cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum.CANCELLED
                || status == cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum.REVIEW_REQUIRED;
    }
}
