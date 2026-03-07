package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 润色请求
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolishRequest {
    /**
     * 原文（可选，用于上下文）
     */
    private String sourceText;

    /**
     * 机器翻译结果（必填）
     */
    private String mtText;

    /**
     * 源语言代码，如 zh/en（可选）
     */
    private String sourceLanguage;

    /**
     * 目标语言代码，如 zh/en（必填建议）
     */
    private String targetLanguage;

    /**
     * 术语分类（可选）
     */
    private String category;

    /**
     * 术语领域（可选）
     */
    private String domain;

    /**
     * 样式预设或风格提示（可选）
     */
    private String style;

    /**
     * 用户ID（可选，用于按用户过滤术语库）
     */
    private String userId;
}