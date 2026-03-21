package cn.net.susan.ai.translation.service.agent;

public interface AgentWorkflowService {

    void submitTextTask(Long taskId);

    void executeTextTask(Long taskId);
}
