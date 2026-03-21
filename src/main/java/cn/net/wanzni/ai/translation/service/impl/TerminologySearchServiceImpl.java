package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.service.TerminologySearchService;
import cn.net.wanzni.ai.translation.util.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 术语库搜索服务实现类
 *
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologySearchServiceImpl implements TerminologySearchService {

    private final TerminologyEntryRepository terminologyEntryRepository;

    @Override
    public List<Map<String, Object>> searchTermTranslations(String sourceTerm, String sourceLanguage, String targetLanguage, Boolean exactMatch) throws Exception {
        try {
            log.info("搜索术语翻译: {}", sourceTerm);

            List<TerminologyEntry> entries;
            if (exactMatch != null && exactMatch) {
                Optional<TerminologyEntry> entry = terminologyEntryRepository
                        .findBySourceTermAndSourceLanguageAndTargetLanguage(sourceTerm, sourceLanguage, targetLanguage);
                entries = entry.map(Collections::singletonList).orElse(Collections.emptyList());
            } else {
                entries = terminologyEntryRepository.findBySourceLanguageAndTargetLanguageAndIsActiveTrue(
                        sourceLanguage, targetLanguage, Pageable.unpaged()).getContent();
                entries = entries.stream()
                        .filter(e -> e.getSourceTerm().toLowerCase().contains(sourceTerm.toLowerCase()))
                        .toList();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (TerminologyEntry entry : entries) {
                Map<String, Object> result = new HashMap<>();
                result.put("sourceTerm", entry.getSourceTerm());
                result.put("targetTerm", entry.getTargetTerm());
                result.put("category", entry.getCategory());
                result.put("domain", entry.getDomain());
                result.put("notes", entry.getNotes());
                results.add(result);
            }

            return results;
        } catch (Exception e) {
            log.error("搜索术语翻译失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, List<Map<String, Object>>> batchSearchTermTranslations(List<String> sourceTerms, String sourceLanguage, String targetLanguage) throws Exception {
        try {
            log.info("批量搜索术语翻译，数量: {}", sourceTerms.size());

            List<TerminologyEntry> entries = terminologyEntryRepository
                    .findBySourceTermsAndLanguagePair(sourceTerms, sourceLanguage, targetLanguage);

            Map<String, List<Map<String, Object>>> results = new HashMap<>();
            for (TerminologyEntry entry : entries) {
                Map<String, Object> result = new HashMap<>();
                result.put("sourceTerm", entry.getSourceTerm());
                result.put("targetTerm", entry.getTargetTerm());
                result.put("category", entry.getCategory());
                result.put("domain", entry.getDomain());
                result.put("notes", entry.getNotes());

                results.computeIfAbsent(entry.getSourceTerm(), k -> new ArrayList<>()).add(result);
            }

            for (String sourceTerm : sourceTerms) {
                if (!results.containsKey(sourceTerm)) {
                    results.put(sourceTerm, new ArrayList<>());
                }
            }

            return results;
        } catch (Exception e) {
            log.error("批量搜索术语翻译失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<TerminologyEntry> searchTerminologyEntries(String keyword, String sourceLanguage, String targetLanguage, String category, String domain, String createdBy, Pageable pageable) {
        log.debug("Searching for terminology entries with keyword: {}", keyword);
        Long userId = UserContextUtils.safeParseUserId(createdBy);
        TerminologyEntry.TerminologyCategory parsedCategory = category != null ? TerminologyEntry.TerminologyCategory.valueOf(category) : null;
        return terminologyEntryRepository.findByKeywordAndFilters(
                keyword, sourceLanguage, targetLanguage, parsedCategory, domain, userId, pageable
        );
    }

    /**
     * @return
     */
    @Override
    public List<Map<String, Object>> findSimilarTerms(String sourceTerm, String sourceLanguage, String targetLanguage, Double similarity) throws Exception {
        try {
            log.info("查找相似术语: {}", sourceTerm);

            List<Map<String, Object>> results = new ArrayList<>();
            return results;
        } catch (Exception e) {
            log.error("查找相似术语失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Map<String, Object>> getTermTranslationSuggestions(String sourceTerm, String sourceLanguage, String targetLanguage, String context) throws Exception {
        try {
            log.info("获取术语翻译建议: {}", sourceTerm);

            List<Map<String, Object>> suggestions = new ArrayList<>();
            return suggestions;
        } catch (Exception e) {
            log.error("获取术语翻译建议失败: {}", e.getMessage());
            throw e;
        }
    }
}