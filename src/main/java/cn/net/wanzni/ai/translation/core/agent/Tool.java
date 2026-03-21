package cn.net.wanzni.ai.translation.core.agent;

import java.util.Map;

public interface Tool {

    String getName();

    String getDescription();

    Map<String, ToolParam> getParams();

    ToolResult execute(Map<String, Object> params) throws Exception;

    record ToolParam(String name, String type, String description, boolean required) {
    }

    record ToolResult(boolean success, String output, String error, Long durationMs) {
        public static ToolResult ok(String output) {
            return new ToolResult(true, output, null, null);
        }

        public static ToolResult ok(String output, Long durationMs) {
            return new ToolResult(true, output, null, durationMs);
        }

        public static ToolResult fail(String error) {
            return new ToolResult(false, null, error, null);
        }
    }
}