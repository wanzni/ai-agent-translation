package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.io.Serializable;

/**
 * 支持语言响应DTO
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportedLanguageResponse implements Serializable {

    /**
     * 语言代码
     */
    private String languageCode;

    /**
     * 语言名称
     */
    private String languageName;

    /**
     * 本地语言名称
     */
    private String nativeName;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
