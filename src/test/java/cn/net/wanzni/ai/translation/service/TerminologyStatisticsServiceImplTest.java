package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.dto.TerminologyStatsResponse;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.service.impl.TerminologyStatisticsServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerminologyStatisticsServiceImplTest {

    @Test
    void shouldAggregateStatsFromUserEntries() throws Exception {
        TerminologyEntryRepository repository = mock(TerminologyEntryRepository.class);
        TerminologyStatisticsServiceImpl service = new TerminologyStatisticsServiceImpl(repository);

        when(repository.findByUserId(8L)).thenReturn(List.of(
                entry(1L, "防水", "waterproof", TerminologyEntry.TerminologyCategory.BUSINESS, "zh", "en", true),
                entry(2L, "轻量", "lightweight", TerminologyEntry.TerminologyCategory.BUSINESS, "zh", "en", true),
                entry(3L, "AMOLED", "AMOLED", TerminologyEntry.TerminologyCategory.TECHNOLOGY, "zh", "en", true),
                entry(4L, "已停用", "deprecated", TerminologyEntry.TerminologyCategory.GENERAL, "zh", "en", false)
        ));

        TerminologyStatsResponse response = service.getTerminologyStatisticsResponse("8");

        assertEquals(3L, response.getTotalEntries());
        assertEquals(3L, response.getTotalTerms());
        assertEquals(2, response.getCategoryCount());
        assertEquals(2, response.getCategoryCounts().size());
        assertEquals("BUSINESS", response.getCategoryCounts().get(0).getCategory());
        assertEquals(2L, response.getCategoryCounts().get(0).getCount());
        assertEquals("TECHNOLOGY", response.getCategoryCounts().get(1).getCategory());
        assertEquals(1L, response.getCategoryCounts().get(1).getCount());
        assertEquals(1, response.getLanguagePairCounts().size());
        assertEquals("zh", response.getLanguagePairCounts().get(0).getSourceLanguage());
        assertEquals("en", response.getLanguagePairCounts().get(0).getTargetLanguage());
        assertEquals(3L, response.getLanguagePairCounts().get(0).getCount());
    }

    private TerminologyEntry entry(Long id,
                                   String sourceTerm,
                                   String targetTerm,
                                   TerminologyEntry.TerminologyCategory category,
                                   String sourceLanguage,
                                   String targetLanguage,
                                   boolean active) {
        return TerminologyEntry.builder()
                .id(id)
                .sourceTerm(sourceTerm)
                .targetTerm(targetTerm)
                .category(category)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .userId(8L)
                .isActive(active)
                .build();
    }
}
