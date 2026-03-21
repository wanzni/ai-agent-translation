package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.susan.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.susan.ai.translation.dto.agent.AgentTaskStepResponse;
import cn.net.susan.ai.translation.service.agent.AgentTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agent/tasks")
@RequiredArgsConstructor
@Validated
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

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
}
