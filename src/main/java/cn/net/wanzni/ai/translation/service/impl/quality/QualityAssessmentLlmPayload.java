package cn.net.wanzni.ai.translation.service.impl.quality;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record QualityAssessmentLlmPayload(
        Integer overallScore,
        Integer accuracyScore,
        Integer fluencyScore,
        Integer consistencyScore,
        Integer completenessScore,
        List<String> improvementSuggestions,
        List<String> attentionPoints,
        List<String> strengths,
        Map<String, Object> assessmentDetails
) {
}
