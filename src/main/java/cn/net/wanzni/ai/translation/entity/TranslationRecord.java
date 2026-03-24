package cn.net.wanzni.ai.translation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 翻译记录实体类
 * 
 * 存储用户的翻译历史记录，包括源文本、翻译结果、质量评分等信息
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "translation_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationRecord {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID（可选，支持匿名翻译）
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 源语言代码
     */
    @Column(name = "source_language", nullable = false, length = 10)
    private String sourceLanguage;

    /**
     * 目标语言代码
     */
    @Column(name = "target_language", nullable = false, length = 10)
    private String targetLanguage;

    /**
     * 源文本内容
     */
    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    /**
     * 翻译结果
     */
    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    /**
     * 翻译类型（TEXT, DOCUMENT, CHAT）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "translation_type", nullable = false)
    private TranslationType translationType;

    /**
     * 翻译引擎（ALIYUN, DASHSCOPE, OPENAI等）
     */
    @Column(name = "translation_engine", length = 50)
    private String translationEngine;

    /**
     * 质量评分（0-100）
     */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /**
     * 翻译耗时（毫秒）
     */
    @Column(name = "processing_time")
    private Long processingTime;

    /**
     * 字符数统计
     */
    @Column(name = "character_count")
    private Integer characterCount;

    /**
     * 是否使用术语库
     */
    @Column(name = "use_terminology")
    @Builder.Default
    private Boolean useTerminology = false;

    /**
     * 翻译状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TranslationStatus status = TranslationStatus.COMPLETED;

    /**
     * 错误信息（如果翻译失败）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 关联的 Agent 任务ID
     */
    @Column(name = "agent_task_id")
    private Long agentTaskId;

    /**
     * 本次翻译命中的 TM 数量
     */
    @Column(name = "tm_hit_count")
    @Builder.Default
    private Integer tmHitCount = 0;

    /**
     * 工作流工具链路信息
     */
    @Column(name = "tool_trace_json", columnDefinition = "LONGTEXT")
    private String toolTraceJson;

    /**
     * 实际路由使用的模型
     */
    @Column(name = "route_model", length = 64)
    private String routeModel;

    /**
     * 人工复核状态
     */
    @Column(name = "review_status", length = 32)
    private String reviewStatus;

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
     * 翻译类型枚举
     */
    public enum TranslationType {
        TEXT("文本翻译"),
        DOCUMENT("文档翻译"),
        CHAT("对话翻译");

        private final String description;

        TranslationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 翻译状态枚举
     */
    public enum TranslationStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败");

        private final String description;

        TranslationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
