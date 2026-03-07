package cn.net.susan.ai.translation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 术语创建请求
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyCreateRequest {
    /**
     * 源术语
     */
    @NotBlank(message = "源术语不能为空")
    private String sourceTerm;

    /**
     * 目标术语
     */
    @NotBlank(message = "目标术语不能为空")
    private String targetTerm;

    /**
     * 源语言代码
     */
    @NotBlank(message = "源语言不能为空")
    private String sourceLanguage;

    /**
     * 目标语言代码
     */
    @NotBlank(message = "目标语言不能为空")
    private String targetLanguage;

    /**
     * 术语分类（如 TECHNOLOGY、BUSINESS 等），默认 GENERAL
     */
    private String category;

    /**
     * 领域标签（可选）
     */
    private String domain;

    /**
     * 术语定义或备注（可选）
     */
    private String definition;

    /**
     * 使用上下文（可选）
     */
    private String context;

    /**
     * 创建者ID（可选），不传则使用首页默认用户
     */
    private String createdBy;
}