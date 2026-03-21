package cn.net.wanzni.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 翻译响应数据传输对象
 * 
 * 用于返回翻译结果给客户端
 * 
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResponse {

    /**
     * 翻译记录ID
     */
    private Long translationId;

    /**
     * 源文本
     */
    private String sourceText;

    /**
     * 翻译结果
     */
    private String translatedText;

    /**
     * 源语言
     */
    private String sourceLanguage;

    /**
     * 目标语言
     */
    private String targetLanguage;

    /**
     * 翻译类型
     */
    private String translationType;

    /**
     * 翻译引擎
     */
    private String translationEngine;

    /**
     * 翻译耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 字符数
     */
    private Integer characterCount;

    /**
     * 是否使用了术语库
     */
    private Boolean usedTerminology;

    /**
     * 翻译状态
     */
    private String status;

    /**
     * 质量评分（0-100）
     */
    private Double qualityScore;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 置信度（0-1）
     */
    private Double confidence;

    /**
     * 使用的术语数量
     */
    private Integer terminologyCount;

    /**
     * 语言检测结果（如果源语言是自动检测）
     */
    private String detectedLanguage;

    /**
     * 替代翻译建议
     */
    private String[] alternatives;

    /**
     * 翻译建议和提示
     */
    private String suggestions;

    /**
     * 本次实际扣减的点数（优先消耗会员配额，点数为剩余部分）
     */
    private Long usedPoints;

    /**
     * 扣减后的点数余额（若未登录或未扣减则为当前余额或0）
     */
    private Long pointsBalance;

    /**
     * 检查翻译是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(status) && translatedText != null && !translatedText.trim().isEmpty();
    }

    /**
     * 判断是否为高质量翻译
     * 
     * @return 是否高质量
     */
    public boolean isHighQuality() {
        return qualityScore != null && qualityScore >= 80.0;
    }

    /**
     * 获取翻译效率（字符/秒）
     * 
     * @return 翻译效率
     */
    public Double getTranslationEfficiency() {
        if (processingTime != null && processingTime > 0 && characterCount != null && characterCount > 0) {
            return (double) characterCount / (processingTime / 1000.0);
        }
        return null;
    }

    /**
     * 获取语言对标识
     * 
     * @return 语言对字符串
     */
    public String getLanguagePair() {
        return sourceLanguage + "-" + targetLanguage;
    }
}