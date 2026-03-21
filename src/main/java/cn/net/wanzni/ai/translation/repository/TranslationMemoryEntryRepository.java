package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.TranslationMemoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranslationMemoryEntryRepository extends JpaRepository<TranslationMemoryEntry, Long> {

    Page<TranslationMemoryEntry> findBySourceLanguageAndTargetLanguageOrderByUpdatedAtDesc(
            String sourceLanguage, String targetLanguage, Pageable pageable);
}
