package cn.net.susan.ai.translation.dto;

import lombok.Data;

/**
 * 文档翻译统计信息的数据传输对象。
 */
@Data
public class DocumentTranslationStatisticsDTO {
    /** 总文档数 */
    private long totalDocuments;
    /** 已完成的文档数 */
    private long completedDocuments;
    /** 失败的文档数 */
    private long failedDocuments;
    /** 总文件大小 (MB) */
    private double totalFileSizeMb;
    /** 平均处理时间 (毫秒) */
    private long averageProcessingTimeMillis;
    /** 翻译最多的文档类型 */
    private String mostTranslatedDocumentType;

    public DocumentTranslationStatisticsDTO(long totalDocuments, long completedDocuments, long failedDocuments, double totalFileSizeMb, long averageProcessingTimeMillis, String mostTranslatedDocumentType) {
        this.totalDocuments = totalDocuments;
        this.completedDocuments = completedDocuments;
        this.failedDocuments = failedDocuments;
        this.totalFileSizeMb = totalFileSizeMb;
        this.averageProcessingTimeMillis = averageProcessingTimeMillis;
        this.mostTranslatedDocumentType = mostTranslatedDocumentType;
    }
}