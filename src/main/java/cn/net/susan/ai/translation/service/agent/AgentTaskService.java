package cn.net.susan.ai.translation.service.agent;

import cn.net.susan.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.susan.ai.translation.dto.agent.AgentTaskResponse;

public interface AgentTaskService {

    AgentTaskResponse createTextTask(AgentTaskCreateRequest request);

    AgentTaskResponse getTask(Long taskId);
}
