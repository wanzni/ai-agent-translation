package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 翻译预估DTO
 *
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationEstimateDTO {

    /**
     * 字符数
     */
    private Long characterCount;
    /**
     * 预计时间（分钟）
     */
    private Integer estimatedTimeMinutes;
    /**
     * 预计费用
     */
    private Double estimatedCost;
    /**
     * 是否支持
     */
    private Boolean supported;
}