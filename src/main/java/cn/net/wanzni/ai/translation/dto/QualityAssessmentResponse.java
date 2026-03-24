package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 质量评估响应
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityAssessmentResponse {
    /**
     * 综合得分
     */
    private Integer overallScore;
    /**
     * 准确性得分
     */
    private Integer accuracyScore;
    /**
     * 流畅性得分
     */
    private Integer fluencyScore;
    /**
     * 一致性得分
     */
    private Integer consistencyScore;
    /**
     * 完整性得分
     */
    private Integer completenessScore;

    /**
     * 改进建议
     */
    private List<String> improvementSuggestions;
    /**
     * 注意事项
     */
    private List<String> attentionPoints;
    /**
     * 优点
     */
    private List<String> strengths;

    /**
     * 评估详情
     */
    private Map<String, Object> assessmentDetails;

    /**
     * 评估时间
     */
    private Long assessmentTime;
    /**
     * 评估引擎
     */
    private String assessmentEngine;
    /**
     * 质量等级
     */
    private String qualityLevel;
    /**
     * 检测到的源语言
     */
    private String detectedSourceLanguage;

    private Integer numberScore;

    private Integer terminologyScore;

    private Integer formatScore;

    private Integer llmJudgeScore;

    private Boolean needsHumanReview;

    private Boolean needsRetry;

    private Boolean tmEligible;

    private Boolean hardRulePassed;

    private Boolean sensitiveContentDetected;

    private List<String> tmRejectReasons;
}
