package cn.net.susan.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档翻译统计信息响应DTO
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStatisticsResponse {

    /**
     * 总文档翻译次数
     */
    private Long totalDocuments;

    /**
     * 总文件大小（字节）
     */
    private Long totalFileSize;

    /**
     * 平均处理时间（毫秒）
     */
    private Long averageProcessingTime;

    /**
     * 今日文档翻译次数
     */
    private Long todayDocuments;

    /**
     * 本周文档翻译次数
     */
    private Long weekDocuments;

    /**
     * 本月文档翻译次数
     */
    private Long monthDocuments;

    /**
     * 文档类型统计
     */
    private List<DocumentTypeStats> documentTypeStats;

    /**
     * 处理状态统计
     */
    private List<StatusStats> statusStats;

    /**
     * 平均质量评分
     */
    private Double averageQualityScore;

    /**
     * 下载次数统计
     */
    private Long totalDownloads;

    /**
     * 统计时间范围
     */
    private LocalDateTime startTime;

    /**
     * 统计时间范围
     */
    private LocalDateTime endTime;

    /**
     * 文档类型统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentTypeStats {
        private String documentType;
        private Long count;
        private Double percentage;
        private Long totalSize;
    }

    /**
     * 状态统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusStats {
        private String status;
        private Long count;
        private Double percentage;
    }
}
