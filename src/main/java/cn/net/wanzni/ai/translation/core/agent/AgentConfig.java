package cn.net.wanzni.ai.translation.core.agent;

import cn.net.wanzni.ai.translation.core.agent.tools.TerminologyTool;
import cn.net.wanzni.ai.translation.core.agent.tools.TranslationTool;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class AgentConfig {

    private final ToolRegistry toolRegistry;
    private final TerminologyTool terminologyTool;
    private final TranslationTool translationTool;

    @PostConstruct
    public void registerTools() {
        toolRegistry.register(terminologyTool);
        toolRegistry.register(translationTool);
    }
}