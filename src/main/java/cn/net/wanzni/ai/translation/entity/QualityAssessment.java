package cn.net.wanzni.ai.translation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 翻译质量评估实体类
 * 
 * 存储翻译质量评估结果，包括各维度评分、改进建议等
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "quality_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityAssessment {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的翻译记录ID
     */
    @Column(name = "translation_record_id", nullable = false)
    private Long translationRecordId;

    /**
     * 评估模式
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "assessment_mode", nullable = false)
    private AssessmentMode assessmentMode;

    /**
     * 整体质量评分（0-100）
     */
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    /**
     * 准确性评分（0-100）
     */
    @Column(name = "accuracy_score", nullable = false)
    private Integer accuracyScore;

    /**
     * 流畅性评分（0-100）
     */
    @Column(name = "fluency_score", nullable = false)
    private Integer fluencyScore;

    /**
     * 一致性评分（0-100）
     */
    @Column(name = "consistency_score", nullable = false)
    private Integer consistencyScore;

    /**
     * 完整性评分（0-100）
     */
    @Column(name = "completeness_score", nullable = false)
    private Integer completenessScore;

    /**
     * 改进建议（JSON格式存储）
     */
    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    /**
     * 注意事项（JSON格式存储）
     */
    @Column(name = "attention_points", columnDefinition = "TEXT")
    private String attentionPoints;

    /**
     * 优点总结（JSON格式存储）
     */
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    /**
     * 评估详情（JSON格式存储详细分析）
     */
    @Column(name = "assessment_details", columnDefinition = "TEXT")
    private String assessmentDetails;

    /**
     * 评估耗时（毫秒）
     */
    @Column(name = "assessment_time")
    private Long assessmentTime;

    /**
     * 评估引擎
     */
    @Column(name = "assessment_engine", length = 50)
    private String assessmentEngine;

    /**
     * 是否人工评估
     */
    @Column(name = "is_manual_assessment")
    @Builder.Default
    private Boolean isManualAssessment = false;

    /**
     * 评估者ID（如果是人工评估）
     */
    @Column(name = "assessor_id", length = 100)
    private String assessorId;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 评估模式枚举
     */
    public enum AssessmentMode {
        AUTOMATIC("自动评估"),
        MANUAL("人工评估"),
        HYBRID("混合评估");

        private final String description;

        AssessmentMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取质量等级
     * 
     * @return 质量等级描述
     */
    public String getQualityLevel() {
        if (overallScore >= 90) {
            return "优秀";
        } else if (overallScore >= 80) {
            return "良好";
        } else if (overallScore >= 70) {
            return "中等";
        } else if (overallScore >= 60) {
            return "及格";
        } else {
            return "需要改进";
        }
    }

    /**
     * 计算整体评分
     * 基于各维度评分的加权平均
     */
    public void calculateOverallScore() {
        // 权重分配：准确性40%，流畅性30%，一致性20%，完整性10%
        double weightedScore = accuracyScore * 0.4 + 
                              fluencyScore * 0.3 + 
                              consistencyScore * 0.2 + 
                              completenessScore * 0.1;
        this.overallScore = (int) Math.round(weightedScore);
    }
}