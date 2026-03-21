package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 术语库统计响应
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyStatsResponse {
    /**
     * 总条目数
     */
    private long totalEntries;
    /**
     * 分类统计
     */
    private List<CategoryCountDTO> categoryCounts;
    /**
     * 语言对统计
     */
    private List<LanguagePairCountDTO> languagePairCounts;
    // 兼容前端的冗余字段
    /**
     * 总术语数
     */
    private Long totalTerms;      // = totalEntries
    /**
     * 分类数
     */
    private Integer categoryCount; // = categoryCounts.size()
}