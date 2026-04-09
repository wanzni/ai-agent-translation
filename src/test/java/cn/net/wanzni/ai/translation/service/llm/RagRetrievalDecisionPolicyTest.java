package cn.net.wanzni.ai.translation.service.llm;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalDecisionPolicyTest {

    private final RagRetrievalDecisionPolicy policy = new RagRetrievalDecisionPolicy();

    @Test
    void shouldSkipHistoryForShortGenericTextWithoutDomain() {
        TranslationRequest request = TranslationRequest.builder()
                .sourceText("good morning")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .useTerminology(true)
                .useRag(true)
                .build();

        RagRetrievalPlan plan = policy.decide(request, List.of("good", "morning"));

        assertTrue(plan.retrieveTerminology());
        assertFalse(plan.retrieveHistory());
        assertTrue(plan.reasons().contains("SKIP_HISTORY_SHORT_GENERIC_TEXT"));
    }

    @Test
    void shouldEnableHistoryWhenDomainIsPresentEvenForShortText() {
        TranslationRequest request = TranslationRequest.builder()
                .sourceText("battery")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .domain("ecommerce")
                .useTerminology(true)
                .useRag(true)
                .build();

        RagRetrievalPlan plan = policy.decide(request, List.of("battery"));

        assertTrue(plan.retrieveTerminology());
        assertTrue(plan.retrieveHistory());
        assertTrue(plan.reasons().contains("DOMAIN_PRESENT"));
        assertTrue(plan.reasons().contains("ENABLE_HISTORY_RETRIEVAL"));
    }

    @Test
    void shouldExplainWhyTerminologyWasSkippedWhenNoKeywordExists() {
        TranslationRequest request = TranslationRequest.builder()
                .sourceText("a")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .useTerminology(true)
                .useRag(false)
                .build();

        RagRetrievalPlan plan = policy.decide(request, List.of());

        assertFalse(plan.retrieveTerminology());
        assertFalse(plan.retrieveHistory());
        assertTrue(plan.reasons().contains("SKIP_TERMINOLOGY_NO_KEYWORD"));
    }
}
