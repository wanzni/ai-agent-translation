package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.TranslationMemoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationMemoryEntryRepository extends JpaRepository<TranslationMemoryEntry, Long> {

    Page<TranslationMemoryEntry> findBySourceLanguageAndTargetLanguageOrderByUpdatedAtDesc(
            String sourceLanguage, String targetLanguage, Pageable pageable);

    Page<TranslationMemoryEntry> findBySourceLanguageAndTargetLanguageAndApprovedTrueOrderByUpdatedAtDesc(
            String sourceLanguage, String targetLanguage, Pageable pageable);

    Page<TranslationMemoryEntry> findBySourceLanguageAndTargetLanguageAndDomainAndApprovedTrueOrderByUpdatedAtDesc(
            String sourceLanguage, String targetLanguage, String domain, Pageable pageable);

    List<TranslationMemoryEntry> findBySourceTextHashAndSourceLanguageAndTargetLanguage(
            String sourceTextHash, String sourceLanguage, String targetLanguage);

    Optional<TranslationMemoryEntry> findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomain(
            String sourceTextHash, String sourceLanguage, String targetLanguage, String domain);

    Optional<TranslationMemoryEntry> findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomainIsNull(
            String sourceTextHash, String sourceLanguage, String targetLanguage);
}
