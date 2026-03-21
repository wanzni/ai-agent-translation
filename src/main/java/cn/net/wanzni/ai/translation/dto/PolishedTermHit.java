package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 润色中术语命中统计
 *
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolishedTermHit {
    /**
     * 术语目标词
     */
    private String term;
    /**
     * 命中次数
     */
    private int count;
}