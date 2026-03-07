package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语言对统计条目
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LanguagePairCountDTO {
    /**
     * 源语言
     */
    private String sourceLanguage;

    /**
     * 目标语言
     */
    private String targetLanguage;

    /**
     * 数量
     */
    private long count;
}