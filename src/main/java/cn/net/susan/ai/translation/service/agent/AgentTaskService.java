package cn.net.susan.ai.translation.service.agent;

import cn.net.susan.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.susan.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.susan.ai.translation.dto.agent.AgentTaskStepResponse;

import java.util.List;

public interface AgentTaskService {

    AgentTaskResponse createTextTask(AgentTaskCreateRequest request);

    AgentTaskResponse getTask(Long taskId);

    List<AgentTaskStepResponse> getTaskSteps(Long taskId);
}
