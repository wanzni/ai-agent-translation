package cn.net.wanzni.ai.translation.service.agent;

import cn.net.wanzni.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskStepResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskTimelineResponse;

import java.util.List;

public interface AgentTaskService {

    AgentTaskResponse createTextTask(AgentTaskCreateRequest request);

    AgentTaskResponse getTask(Long taskId);

    List<AgentTaskStepResponse> getTaskSteps(Long taskId);

    AgentTaskTimelineResponse getTaskTimeline(Long taskId);
}
