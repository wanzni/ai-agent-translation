package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 开始翻译响应
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartTranslationResponse {
    /**
     * 翻译任务ID
     */
    private Long id;
    /**
     * 原始文件名
     */
    private String originalFilename;
    /**
     * 文件大小
     */
    private Long fileSize;
    /**
     * 源语言
     */
    private String sourceLanguage;
    /**
     * 目标语言
     */
    private String targetLanguage;
    /**
     * 翻译引擎
     */
    private String translationEngine;
    /**
     * 状态
     */
    private String status;
    /**
     * 进度
     */
    private Integer progress;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 使用的点数
     */
    private Long usedPoints;
    /**
     * 点数余额
     */
    private Long pointsBalance;
}