package cn.net.susan.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 术语库统计信息响应DTO
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyStatisticsResponse {

    /**
     * 总术语数量
     */
    private Long totalTerms;

    /**
     * 活跃术语数量
     */
    private Long activeTerms;

    /**
     * 分类数量
     */
    private Long categoryCount;

    /**
     * 领域数量
     */
    private Long domainCount;

    /**
     * 最常用术语
     */
    private List<TermUsage> topUsedTerms;

    /**
     * 分类统计
     */
    private List<CategoryStats> categoryStats;

    /**
     * 领域统计
     */
    private List<DomainStats> domainStats;

    /**
     * 语言对统计
     */
    private List<LanguagePairStats> languagePairStats;

    /**
     * 最近添加的术语
     */
    private List<RecentTerm> recentTerms;

    /**
     * 统计时间
     */
    private LocalDateTime statisticsTime;

    /**
     * 术语使用统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TermUsage {
        private String sourceTerm;
        private String targetTerm;
        private Integer usageCount;
        private String category;
    }

    /**
     * 分类统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryStats {
        private String category;
        private Long count;
        private Double percentage;
    }

    /**
     * 领域统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DomainStats {
        private String domain;
        private Long count;
        private Double percentage;
    }

    /**
     * 语言对统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LanguagePairStats {
        private String sourceLanguage;
        private String targetLanguage;
        private Long count;
        private Double percentage;
    }

    /**
     * 最近添加的术语
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentTerm {
        private Long id;
        private String sourceTerm;
        private String targetTerm;
        private String category;
        private LocalDateTime createdAt;
    }
}
