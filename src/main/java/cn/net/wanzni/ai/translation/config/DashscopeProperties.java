package cn.net.wanzni.ai.translation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 灵骏（DashScope）服务配置属性
 *
 * <p>用于配置与阿里云灵骏（DashScope）大模型服务相关的参数，
 * 包括API密钥、模型名称、基础URL等。
 *
 * @version 1.0.0
 * @since 2025-11-21
 */
@Configuration
@ConfigurationProperties(prefix = "ai.dashscope")
public class DashscopeProperties {

    /** DashScope Chat Completions 基础URL（兼容OpenAI接口模式） */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /** 模型名称，例如 qwen-plus / qwen-turbo / qwen-max */
    private String model = "qwen-plus";

    /** DashScope API Key（必填） */
    private String apiKey;

    /** 温度（可选） */
    private Double temperature = 0.2;

    /**
     * 嵌套聊天配置
     * <p>
     * 支持在 application.yml 中通过 `ai.dashscope.chat.options.model`
     * 对特定聊天场景进行更精细的模型配置。
     */
    private Chat chat = new Chat();

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }

    /**
     * 解析并获取最终生效的模型名称。
     *
     * <p>该方法遵循以下优先级规则：
     * <ol>
     *   <li>如果定义了 `ai.dashscope.chat.options.model`，则优先使用该模型。</li>
     *   <li>否则，使用顶层的 `ai.dashscope.model` 作为默认模型。</li>
     * </ol>
     *
     * @return 最终解析出的模型名称。
     */
    public String resolveModel() {
        String nestedModel = (chat != null && chat.options != null) ? chat.options.model : null;
        return StringUtils.hasText(nestedModel) ? nestedModel : model;
    }

    /**
     * 聊天特定配置项
     * <p>
     * 允许在 `chat` 命名空间下定义更具体的配置，
     * 例如为聊天功能指定独立的模型。
     */
    public static class Chat {
        private Options options = new Options();

        public Options getOptions() { return options; }
        public void setOptions(Options options) { this.options = options; }
    }

    /**
     * 聊天选项
     * <p>
     * 包含聊天功能的可配置参数，如模型名称。
     */
    public static class Options {
        /**
         * 嵌套的模型配置
         * <p>
         * 用于覆盖顶层模型设置，为特定聊天场景指定模型。
         * 例如：`ai.dashscope.chat.options.model=qwen-max`
         */
        private String model;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}