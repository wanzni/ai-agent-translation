package cn.net.susan.ai.translation.core.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import cn.net.susan.ai.translation.core.agent.Tool.ToolResult;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("ToolRegistry initialized with {} tools: {}",
                tools.size(), tools.keySet());
    }

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        log.info("Registered tool: {}", tool.getName());
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public List<Tool> getAll() {
        return new ArrayList<>(tools.values());
    }

    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public ToolResult execute(String toolName, Map<String, Object> params) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            return Tool.ToolResult.fail("Tool not found: " + toolName);
        }
        try {
            long start = System.currentTimeMillis();
            ToolResult result = tool.execute(params);
            long duration = System.currentTimeMillis() - start;
            log.info("Tool executed: tool={}, duration={}ms, success={}",
                    toolName, duration, result.success());
            return Tool.ToolResult.ok(result.output(), duration);
        } catch (Exception e) {
            log.error("Tool execution failed: tool={}, error={}", toolName, e.getMessage());
            return Tool.ToolResult.fail(e.getMessage());
        }
    }
}