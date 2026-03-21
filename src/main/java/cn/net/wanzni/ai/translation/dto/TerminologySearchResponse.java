package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 术语搜索响应DTO
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologySearchResponse {

    /**
     * 术语ID
     */
    private Long id;

    /**
     * 源术语
     */
    private String sourceTerm;

    /**
     * 目标翻译
     */
    private String targetTerm;

    /**
     * 源语言
     */
    private String sourceLanguage;

    /**
     * 目标语言
     */
    private String targetLanguage;

    /**
     * 术语分类
     */
    private String category;

    /**
     * 领域标签
     */
    private String domain;

    /**
     * 使用频率
     */
    private Integer usageCount;

    /**
     * 匹配度（0-1）
     */
    private Double matchScore;

    /**
     * 备注信息
     */
    private String notes;

    /**
     * 是否启用
     */
    private Boolean isActive;
}
