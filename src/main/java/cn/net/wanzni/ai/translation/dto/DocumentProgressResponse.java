package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 文档翻译进度响应DTO
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentProgressResponse {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 翻译进度（0-100）
     */
    private Integer progress;

    /**
     * 处理状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusMessage;

    /**
     * 总页数/段落数
     */
    private Integer totalPages;

    /**
     * 已处理页数/段落数
     */
    private Integer processedPages;

    /**
     * 预计完成时间
     */
    private LocalDateTime estimatedCompletionTime;

    /**
     * 已处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 剩余时间（毫秒）
     */
    private Long remainingTime;

    /**
     * 当前处理的页面/段落
     */
    private String currentPage;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 质量评分（如果已完成）
     */
    private Integer qualityScore;

    /**
     * 是否可以使用术语库
     */
    private Boolean useTerminology;

    /**
     * 翻译引擎
     */
    private String translationEngine;
}
