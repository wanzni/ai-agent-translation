package cn.net.wanzni.ai.translation.service.agent;

public interface AgentWorkflowService {

    void submitTextTask(Long taskId);

    void executeTextTask(Long taskId);
}
