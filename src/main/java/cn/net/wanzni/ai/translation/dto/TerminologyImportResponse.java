package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 术语库导入响应DTO
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyImportResponse {

    /**
     * 导入成功数量
     */
    private Integer successCount;

    /**
     * 导入失败数量
     */
    private Integer failureCount;

    /**
     * 总处理数量
     */
    private Integer totalCount;

    /**
     * 导入耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 导入时间
     */
    private LocalDateTime importTime;

    /**
     * 错误信息列表
     */
    private List<ImportError> errors;

    /**
     * 重复术语数量
     */
    private Integer duplicateCount;

    /**
     * 新增术语数量
     */
    private Integer newCount;

    /**
     * 更新术语数量
     */
    private Integer updateCount;

    /**
     * 导入文件信息
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 导入状态
     */
    private String status;

    /**
     * 导入错误信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportError {
        private Integer rowNumber;
        private String errorMessage;
        private String sourceTerm;
        private String targetTerm;
    }
}
