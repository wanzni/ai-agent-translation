package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.io.Serializable;

/**
 * 语言检测响应DTO
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguageDetectionResponse implements Serializable {

    /**
     * 检测到的语言代码
     */
    private String language;

    /**
     * 语言名称
     */
    private String languageName;

    /**
     * 置信度（0-1）
     */
    private Double confidence;

    /**
     * 是否检测成功
     */
    private Boolean success;

    /**
     * 检测耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 错误信息（如果检测失败）
     */
    private String errorMessage;
}
