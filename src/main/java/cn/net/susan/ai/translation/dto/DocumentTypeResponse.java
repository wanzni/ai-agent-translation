package cn.net.susan.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 支持的文档类型响应DTO
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTypeResponse {

    /**
     * 文档类型代码
     */
    private String typeCode;

    /**
     * 文档类型名称
     */
    private String typeName;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 最大文件大小（字节）
     */
    private Long maxFileSize;

    /**
     * 是否支持
     */
    private Boolean isSupported;

    /**
     * 描述
     */
    private String description;
}
