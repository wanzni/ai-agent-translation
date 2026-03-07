package cn.net.susan.ai.translation.entity;

import cn.net.susan.ai.translation.converter.ProcessingStatusEnumConverter;
import cn.net.susan.ai.translation.enums.DocumentTypeEnum;
import cn.net.susan.ai.translation.enums.ProcessingStatusEnum;
import cn.net.susan.ai.translation.enums.TranslationTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档翻译实体类
 * 
 * 存储文档翻译任务的信息，包括文件路径、翻译进度、处理状态等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "document_translations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTranslation {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_type", nullable = false)
    private DocumentTypeEnum fileType;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 源文件路径
     */
    @Column(name = "source_file_path", nullable = false, length = 500)
    private String sourceFilePath;

    /**
     * 翻译后文件路径
     */
    @Column(name = "translated_file_path", length = 500)
    private String translatedFilePath;

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
     * 处理状态
     */
    @Column(name = "status", nullable = false)
    @Convert(converter = ProcessingStatusEnumConverter.class)
    @Builder.Default
    private ProcessingStatusEnum status = ProcessingStatusEnum.PENDING;

    /**
     * 翻译进度（0-100）
     */
    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;

    /**
     * 总页数/段落数
     */
    @Column(name = "total_pages")
    private Integer totalPages;

    /**
     * 已处理页数/段落数
     */
    @Column(name = "processed_pages")
    @Builder.Default
    private Integer processedPages = 0;

    /**
     * 翻译引擎
     */
    @Column(name = "translation_engine", length = 50)
    private String translationEngine;

    /**
     * 是否使用术语库
     */
    @Column(name = "use_terminology")
    @Builder.Default
    private Boolean useTerminology = false;

    /**
     * 质量评分（0-100）
     */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /**
     * 处理耗时（毫秒）
     */
    @Column(name = "processing_time")
    private Long processingTime;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "user_id", insertable = true, updatable = false)
    private Long userId;

    @Column(name = "translation_type")
    private TranslationTypeEnum translationType;

    /**
     * 下载次数
     */
    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

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
     * 开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 翻译内容（二进制）
     * 使用 LONGBLOB 以支持大型文档内容
     */
    @Lob
    @Column(name = "translated_content", columnDefinition = "LONGBLOB")
    private byte[] translatedContent;

    /**
     * 异步任务ID
     */
    @Column(name = "async_task_id")
    private String asyncTaskId;

    /**
     * 优先级
     */
    @Column(name = "priority")
    private Integer priority;

    /**
     * 原始内容
     * 使用 LONGTEXT 以支持较大的纯文本内容
     */
    @Lob
    @Column(name = "original_content", columnDefinition = "LONGTEXT")
    private String originalContent;

    /**
     * 预计完成时间
     */
    @Column(name = "estimated_completion_time")
    private LocalDateTime estimatedCompletionTime;

    /**
     * 最后下载时间
     */
    @Column(name = "last_download_time")
    private LocalDateTime lastDownloadTime;

    /**
     * 状态消息
     */
    @Column(name = "status_message", length = 500)
    private String statusMessage;

    private String failureReason;

    /**
     * 字符数统计
     */
    @Column(name = "character_count")
    private Long characterCount;

    /**
     * 完成时间
     */
    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    /**
     * 完成时间（已存在的字段）
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 更新进度
     * 
     * @param processedPages 已处理页数
     */
    public void updateProgress(int processedPages) {
        this.processedPages = processedPages;
        if (this.totalPages != null && this.totalPages > 0) {
            this.progress = (int) ((double) processedPages / totalPages * 100);
        }
    }

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    // 显式提供关键字段的 setter，确保服务层更新字段可用
    public void setTranslatedFilePath(String translatedFilePath) {
        this.translatedFilePath = translatedFilePath;
    }

    public void setTranslatedContent(byte[] translatedContent) {
        this.translatedContent = translatedContent;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}