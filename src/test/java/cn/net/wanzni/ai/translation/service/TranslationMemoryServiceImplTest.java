package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.entity.TranslationMemoryEntry;
import cn.net.wanzni.ai.translation.repository.TranslationMemoryEntryRepository;
import cn.net.wanzni.ai.translation.service.impl.TranslationMemoryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TranslationMemoryServiceImplTest {

    @Test
    void shouldNotSaveWhenTmGateRejectsCandidate() {
        TranslationMemoryEntryRepository repository = mock(TranslationMemoryEntryRepository.class);
        TranslationMemoryServiceImpl service = new TranslationMemoryServiceImpl(repository, mock(cn.net.wanzni.ai.translation.repository.ReviewTaskRepository.class));

        Optional<TranslationMemoryEntry> result = service.saveApprovedPair(
                "source",
                "target",
                "en",
                "zh",
                "saas",
                95,
                true,
                false,
                false,
                false,
                1L,
                2L
        );

        assertTrue(result.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingEntryOnlyWhenNewVersionHasHigherScore() {
        TranslationMemoryEntryRepository repository = mock(TranslationMemoryEntryRepository.class);
        TranslationMemoryServiceImpl service = new TranslationMemoryServiceImpl(repository, mock(cn.net.wanzni.ai.translation.repository.ReviewTaskRepository.class));

        TranslationMemoryEntry existing = TranslationMemoryEntry.builder()
                .id(10L)
                .sourceText("The API returns 200 ms.")
                .targetText("API 返回 200 ms。")
                .sourceLanguage("en")
                .targetLanguage("zh")
                .domain("saas")
                .sourceTextHash(hashOf(service, "the api returns 200 ms."))
                .qualityScore(88)
                .approved(true)
                .hitCount(1)
                .build();

        when(repository.findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomain(any(), eq("en"), eq("zh"), eq("saas")))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(TranslationMemoryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TranslationMemoryEntry> upgraded = service.saveApprovedPair(
                "The API returns 200 ms.",
                "该 API 会在 200 ms 内返回结果。",
                "en",
                "zh",
                "saas",
                93,
                true,
                false,
                true,
                false,
                1L,
                2L
        );

        assertTrue(upgraded.isPresent());
        assertEquals(93, upgraded.get().getQualityScore());
        assertEquals("该 API 会在 200 ms 内返回结果。", upgraded.get().getTargetText());

        reset(repository);
        when(repository.findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomain(any(), eq("en"), eq("zh"), eq("saas")))
                .thenReturn(Optional.of(existing));

        Optional<TranslationMemoryEntry> kept = service.saveApprovedPair(
                "The API returns 200 ms.",
                "API 200 ms 返回。",
                "en",
                "zh",
                "saas",
                80,
                true,
                false,
                true,
                false,
                1L,
                2L
        );

        assertTrue(kept.isPresent());
        assertEquals(93, kept.get().getQualityScore());
        verify(repository, never()).save(any(TranslationMemoryEntry.class));
    }

    @Test
    void shouldNotOverrideGlobalEntryWhenSavingDomainSpecificVersion() {
        TranslationMemoryEntryRepository repository = mock(TranslationMemoryEntryRepository.class);
        TranslationMemoryServiceImpl service = new TranslationMemoryServiceImpl(repository, mock(cn.net.wanzni.ai.translation.repository.ReviewTaskRepository.class));

        when(repository.findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomain(any(), eq("en"), eq("zh"), eq("saas")))
                .thenReturn(Optional.empty());
        when(repository.save(any(TranslationMemoryEntry.class))).thenAnswer(invocation -> {
            TranslationMemoryEntry entry = invocation.getArgument(0);
            entry.setId(20L);
            return entry;
        });

        Optional<TranslationMemoryEntry> saved = service.saveApprovedPair(
                "The API returns 200 ms.",
                "SaaS API 会在 200 ms 内返回。",
                "en",
                "zh",
                "saas",
                91,
                true,
                false,
                true,
                false,
                1L,
                2L
        );

        assertTrue(saved.isPresent());
        assertEquals("saas", saved.get().getDomain());
        verify(repository, never()).findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomainIsNull(any(), any(), any());
        verify(repository).save(any(TranslationMemoryEntry.class));
    }

    private String hashOf(TranslationMemoryServiceImpl service, String text) {
        try {
            var method = TranslationMemoryServiceImpl.class.getDeclaredMethod("sha256", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
