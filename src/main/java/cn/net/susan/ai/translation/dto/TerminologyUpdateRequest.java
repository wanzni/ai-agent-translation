package cn.net.susan.ai.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 术语更新请求
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyUpdateRequest {
    /**
     * 源术语
     */
    @NotBlank(message = "源术语不能为空")
    @Size(max = 500, message = "源术语长度不能超过500")
    private String sourceTerm;

    /**
     * 目标术语
     */
    @NotBlank(message = "目标术语不能为空")
    @Size(max = 500, message = "目标术语长度不能超过500")
    private String targetTerm;

    /**
     * 术语分类（枚举值），不传则后端兜底为 GENERAL
     */
    @Pattern(
        regexp = "TECHNOLOGY|BUSINESS|MEDICAL|LEGAL|FINANCE|EDUCATION|SCIENCE|GENERAL",
        message = "分类必须是有效枚举值"
    )
    private String category;

    /**
     * 领域标签（可选）
     */
    @Size(max = 100, message = "领域标签长度不能超过100")
    private String domain;

    /**
     * 术语定义或备注（可选），映射到实体 notes
     */
    @Size(max = 10000, message = "备注长度过长")
    private String definition;
}