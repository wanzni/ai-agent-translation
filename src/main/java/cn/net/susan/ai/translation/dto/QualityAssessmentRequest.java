package cn.net.susan.ai.translation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 质量评估请求
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityAssessmentRequest {
    /**
     * 源文本
     */
    @NotBlank
    private String sourceText;

    /**
     * 目标文本
     */
    @NotBlank
    private String targetText;

    /**
     * 源语言，可为 null 或 "auto" 进行自动检测
     */
    private String sourceLanguage;

    /**
     * 目标语言
     */
    @NotBlank
    private String targetLanguage;

    /**
     * 评估模式（默认 AUTOMATIC）
     */
    @Builder.Default
    private String mode = "AUTOMATIC";

    /**
     * 可选：关联翻译记录ID，提供则可持久化评估结果
     */
    private Long translationRecordId;

    /**
     * 是否保存评估结果（需提供 translationRecordId）
     */
    @Builder.Default
    private boolean save = false;

    /**
     * 可选：评估维度列表（默认包含准确性、流畅性、一致性、完整性）
     */
    private List<String> dimensions;
}