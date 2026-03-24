package cn.net.wanzni.ai.translation.service.llm;

import cn.net.wanzni.ai.translation.dto.RagContext;
import cn.net.wanzni.ai.translation.dto.TranslationMemoryMatch;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceFallbackTest {

    @Test
    void shouldFallbackToTranslationRecordsWhenTmIsEmpty() throws Exception {
        TerminologyEntryRepository terminologyEntryRepository = mock(TerminologyEntryRepository.class);
        TranslationMemoryService translationMemoryService = mock(TranslationMemoryService.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.ai.embedding.EmbeddingModel> objectProvider = mock(ObjectProvider.class);
        RagService ragService = new RagService(
                terminologyEntryRepository,
                translationMemoryService,
                translationRecordRepository,
                objectProvider
        );

        when(translationMemoryService.searchSimilar("hello world", "en", "zh", "saas", 2))
                .thenReturn(List.of());
        when(translationRecordRepository.findRagFallbackCandidates(eq("en"), eq("zh"), eq("saas"), any(Pageable.class)))
                .thenReturn(List.of(TranslationRecord.builder()
                        .id(1L)
                        .sourceText("hello world")
                        .translatedText("你好，世界")
                        .createdAt(LocalDateTime.now())
                        .build()));

        List<RagContext.HistorySnippet> snippets = invokeSearchHistorySnippets(ragService, "hello world", "en", "zh", "saas", 2);

        assertEquals(1, snippets.size());
        assertEquals("HISTORY", snippets.get(0).getSourceType());
    }

    @Test
    void shouldNotFallbackWhenTmIsEnough() throws Exception {
        TerminologyEntryRepository terminologyEntryRepository = mock(TerminologyEntryRepository.class);
        TranslationMemoryService translationMemoryService = mock(TranslationMemoryService.class);
        TranslationRecordRepository translationRecordRepository = mock(TranslationRecordRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.ai.embedding.EmbeddingModel> objectProvider = mock(ObjectProvider.class);
        RagService ragService = new RagService(
                terminologyEntryRepository,
                translationMemoryService,
                translationRecordRepository,
                objectProvider
        );

        when(translationMemoryService.searchSimilar("hello world", "en", "zh", "saas", 1))
                .thenReturn(List.of(TranslationMemoryMatch.builder()
                        .sourceText("hello world")
                        .targetText("你好，世界")
                        .sourceType("TM")
                        .build()));

        List<RagContext.HistorySnippet> snippets = invokeSearchHistorySnippets(ragService, "hello world", "en", "zh", "saas", 1);

        assertEquals(1, snippets.size());
        assertEquals("TM", snippets.get(0).getSourceType());
        verify(translationRecordRepository, never()).findRagFallbackCandidates(any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private List<RagContext.HistorySnippet> invokeSearchHistorySnippets(RagService ragService,
                                                                        String sourceText,
                                                                        String sourceLanguage,
                                                                        String targetLanguage,
                                                                        String domain,
                                                                        int topK) throws Exception {
        Method method = RagService.class.getDeclaredMethod(
                "searchHistorySnippets",
                String.class,
                String.class,
                String.class,
                String.class,
                int.class
        );
        method.setAccessible(true);
        Object result = method.invoke(ragService, sourceText, sourceLanguage, targetLanguage, domain, topK);
        assertTrue(result instanceof List<?>);
        return (List<RagContext.HistorySnippet>) result;
    }
}
