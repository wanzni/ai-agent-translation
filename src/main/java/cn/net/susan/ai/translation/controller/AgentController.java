package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.core.agent.TranslationAgent;
import cn.net.susan.ai.translation.dto.TranslationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final TranslationAgent translationAgent;

    @PostMapping("/translate")
    public AgentResponse translate(@RequestBody TranslationRequest request) throws Exception {
        log.info("收到Agent翻译请求: {} -> {}", request.getSourceLanguage(), request.getTargetLanguage());

        TranslationAgent.AgentResponse response = translationAgent.think(request);

        return new AgentResponse(
                response.thought(),
                response.action(),
                response.observation(),
                response.finalResponse(),
                response.toolCalls(),
                response.totalDurationMs()
        );
    }

    public record AgentResponse(
            String thought,
            String action,
            String observation,
            String finalResponse,
            java.util.List<TranslationAgent.AgentResponse.ToolCall> toolCalls,
            long totalDurationMs
    ) {}
}