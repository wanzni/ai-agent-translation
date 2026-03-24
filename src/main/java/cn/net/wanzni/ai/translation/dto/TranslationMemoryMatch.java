package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationMemoryMatch {

    private Long entryId;

    private String sourceText;

    private String targetText;

    private String domain;

    private Integer qualityScore;

    private Integer hitCount;

    private Double similarityScore;

    private Boolean exactMatch;
}
