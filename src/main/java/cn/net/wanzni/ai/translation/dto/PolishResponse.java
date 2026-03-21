package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 润色响应
 *
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolishResponse {
    /**
     * 润色后的文本
     */
    private String polishedText;

    /**
     * 应用的术语命中统计
     */
    private List<PolishedTermHit> termHits;

    /**
     * 命中总数
     */
    private int appliedTermsCount;

    /**
     * 简单质量分（可选）
     */
    private Double qualityScore;

    /**
     * 错误信息（可选）
     */
    private String errorMessage;
}