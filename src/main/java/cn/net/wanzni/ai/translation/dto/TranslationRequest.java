package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 翻译请求数据传输对象
 * 
 * 用于接收客户端的翻译请求参数
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationRequest {

    /**
     * 源文本内容
     */
    @NotBlank(message = "源文本不能为空")
    @Size(max = 10000, message = "源文本长度不能超过10000字符")
    private String sourceText;

    /**
     * 源语言代码
     */
    @NotBlank(message = "源语言不能为空")
    @Size(min = 2, max = 10, message = "语言代码长度必须在2-10字符之间")
    private String sourceLanguage;

    /**
     * 目标语言代码
     */
    @NotBlank(message = "目标语言不能为空")
    @Size(min = 2, max = 10, message = "语言代码长度必须在2-10字符之间")
    private String targetLanguage;

    /**
     * 翻译类型
     */
    @Builder.Default
    private String translationType = "TEXT";

    /**
     * 翻译引擎
     */
    @Builder.Default
    private String translationEngine = "ALIBABA_CLOUD";

    /**
     * 是否使用术语库
     */
    @Builder.Default
    private Boolean useTerminology = true;

    /**
     * 用户ID（可选，用于记录翻译历史）
     */
    private Long userId;

    /**
     * 关联的 Agent 任务ID（可选）
     */
    private Long agentTaskId;

    /**
     * 领域标签（可选，用于术语库匹配）
     */
    private String domain;

    /**
     * 是否需要质量评估
     */
    @Builder.Default
    private Boolean needQualityAssessment = false;

    /**
     * 翻译优先级
     */
    @Builder.Default
    private Integer priority = 1;

    /**
     * 客户端信息（可选）
     * 前端传入对象，例如 { userAgent, platform }
     */
    private java.util.Map<String, Object> clientInfo;

    /**
     * 请求来源（可选）
     */
    private String requestSource;

    /**
     * 是否启用 RAG（检索增强生成）
     */
    @Builder.Default
    private Boolean useRag = true;

    /**
     * RAG 检索 TopK 数量（可选，默认5）
     */
    @Builder.Default
    private Integer ragTopK = 5;

    /**
     * RAG 上下文（服务端构建），包括检索到的片段、术语映射等。
     * 结构示例：
     * {
     *   "contextSnippets": List<String>,
     *   "glossaryMap": Map<String,String>,
     *   "historySnippets": List<Map<String,String>>
     * }
     */
    private Map<String, Object> ragContext;

    /**
     * 验证翻译请求的基本参数
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return sourceText != null && !sourceText.trim().isEmpty()
                && sourceLanguage != null && !sourceLanguage.trim().isEmpty()
                && targetLanguage != null && !targetLanguage.trim().isEmpty()
                && !sourceLanguage.equals(targetLanguage);
    }

    /**
     * 获取源文本字符数
     * 
     * @return 字符数
     */
    public int getCharacterCount() {
        return sourceText != null ? sourceText.length() : 0;
    }

    /**
     * 获取语言对标识
     * 
     * @return 语言对字符串
     */
    public String getLanguagePair() {
        return sourceLanguage + "-" + targetLanguage;
    }

    /**
     * 是否为高优先级请求
     * 
     * @return 是否高优先级
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 3;
    }
}
