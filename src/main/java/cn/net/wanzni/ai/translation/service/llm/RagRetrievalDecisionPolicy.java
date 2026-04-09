package cn.net.wanzni.ai.translation.service.llm;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

class RagRetrievalDecisionPolicy {

    RagRetrievalPlan decide(TranslationRequest request, List<String> tokens) {
        boolean terminologyEnabled = Boolean.TRUE.equals(request.getUseTerminology());
        boolean ragEnabled = Boolean.TRUE.equals(request.getUseRag());
        String sourceText = request.getSourceText();
        int textLength = sourceText == null ? 0 : sourceText.trim().length();
        boolean hasDomain = StringUtils.hasText(request.getDomain());
        boolean hasEnoughTokens = tokens != null && !tokens.isEmpty();
        boolean genericShortText = !hasDomain && textLength > 0 && textLength <= 12 && (tokens == null || tokens.size() <= 2);

        List<String> reasons = new ArrayList<>();
        if (terminologyEnabled) {
            reasons.add("TERMINOLOGY_FEATURE_ENABLED");
        } else {
            reasons.add("TERMINOLOGY_DISABLED");
        }
        if (ragEnabled) {
            reasons.add("RAG_FEATURE_ENABLED");
        } else {
            reasons.add("RAG_DISABLED");
        }
        if (hasDomain) {
            reasons.add("DOMAIN_PRESENT");
        }
        if (hasEnoughTokens) {
            reasons.add("KEYWORDS_EXTRACTED");
        }

        boolean retrieveTerminology = terminologyEnabled && (hasEnoughTokens || hasDomain);
        if (!retrieveTerminology && terminologyEnabled) {
            reasons.add("SKIP_TERMINOLOGY_NO_KEYWORD");
        }

        boolean retrieveHistory = ragEnabled && !genericShortText;
        if (retrieveHistory) {
            reasons.add("ENABLE_HISTORY_RETRIEVAL");
        } else if (ragEnabled) {
            reasons.add("SKIP_HISTORY_SHORT_GENERIC_TEXT");
        }

        return new RagRetrievalPlan(
                retrieveTerminology || retrieveHistory,
                retrieveTerminology,
                retrieveHistory,
                List.copyOf(reasons)
        );
    }
}
