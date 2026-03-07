package cn.net.susan.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 翻译统计信息响应DTO
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationStatisticsResponse {

    /**
     * 总翻译次数
     */
    private Long totalTranslations;

    /**
     * 总字符数
     */
    private Long totalCharacters;

    /**
     * 平均质量评分
     */
    private Double averageQualityScore;

    /**
     * 今日翻译次数
     */
    private Long todayTranslations;

    /**
     * 本周翻译次数
     */
    private Long weekTranslations;

    /**
     * 本月翻译次数
     */
    private Long monthTranslations;

    /**
     * 最常用语言对
     */
    private List<LanguagePairUsage> topLanguagePairs;

    /**
     * 最常用翻译引擎
     */
    private List<EngineUsage> topEngines;

    /**
     * 翻译类型统计
     */
    private TranslationTypeStats translationTypeStats;

    /**
     * 质量评分分布
     */
    private QualityScoreDistribution qualityScoreDistribution;

    /**
     * 统计时间范围
     */
    private LocalDateTime startTime;

    /**
     * 统计时间范围
     */
    private LocalDateTime endTime;

    /**
     * 语言对使用统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LanguagePairUsage {
        private String sourceLanguage;
        private String targetLanguage;
        private Long usageCount;
        private Double percentage;
    }

    /**
     * 引擎使用统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EngineUsage {
        private String engineName;
        private Long usageCount;
        private Double percentage;
    }

    /**
     * 翻译类型统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TranslationTypeStats {
        private Long textTranslations;
        private Long documentTranslations;
        private Long chatTranslations;
    }

    /**
     * 质量评分分布
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QualityScoreDistribution {
        private Long excellentCount; // 90-100
        private Long goodCount;      // 80-89
        private Long fairCount;      // 70-79
        private Long poorCount;      // 60-69
        private Long badCount;       // 0-59
    }
}
