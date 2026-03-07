package cn.net.susan.ai.translation.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 翻译进度DTO
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@Builder
public class TranslationProgressDTO {
    /**
     * 状态
     */
    private String status;
    /**
     * 进度
     */
    private Integer progress;
    /**
     * 消息
     */
    private String message;
    /**
     * 翻译文件URL
     */
    private String translatedFileUrl;
}