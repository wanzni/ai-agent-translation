package cn.net.wanzni.ai.translation.core.agent;

import java.util.List;
import java.util.Map;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;

public interface TranslationAgent {

    AgentResponse think(TranslationRequest request) throws Exception;

    record AgentResponse(
            String thought,
            String action,
            String observation,
            String finalResponse,
            List<ToolCall> toolCalls,
            long totalDurationMs
    ) {
        public record ToolCall(String toolName, Map<String, Object> params, String result) {
        }
    }
}