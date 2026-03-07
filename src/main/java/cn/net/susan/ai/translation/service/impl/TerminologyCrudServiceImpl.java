package cn.net.susan.ai.translation.service.impl;

import cn.net.susan.ai.translation.entity.TerminologyEntry;
import cn.net.susan.ai.translation.repository.TerminologyEntryRepository;
import cn.net.susan.ai.translation.service.TerminologyCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 术语库 CRUD 服务实现类
 *
 * @author sushan
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologyCrudServiceImpl implements TerminologyCrudService {

    private final TerminologyEntryRepository terminologyEntryRepository;

    @Override
    public TerminologyEntry createTerminologyEntry(String sourceTerm, String targetTerm, String sourceLanguage, String targetLanguage, String category, String domain, String definition, String context, String createdBy) throws Exception {
        try {
            log.info("创建术语条目: {}", sourceTerm);
            Long userId = safeParseUserId(createdBy);

            TerminologyEntry.TerminologyCategory parsedCategory;
            try {
                parsedCategory = TerminologyEntry.TerminologyCategory.valueOf(category);
            } catch (IllegalArgumentException ex) {
                parsedCategory = TerminologyEntry.TerminologyCategory.GENERAL;
            }

            Optional<TerminologyEntry> existingByUk = terminologyEntryRepository
                    .findBySourceTermAndSourceLanguageAndTargetLanguage(sourceTerm, sourceLanguage, targetLanguage);

            if (existingByUk.isPresent()) {
                TerminologyEntry exist = existingByUk.get();
                exist.setTargetTerm(targetTerm);
                exist.setCategory(parsedCategory);
                exist.setDomain(domain);
                exist.setNotes(definition);
                exist.setCreatedBy(createdBy);
                exist.setUserId(userId != null ? userId : 0L);
                exist.setUpdatedAt(LocalDateTime.now());
                return terminologyEntryRepository.save(exist);
            }

            TerminologyEntry entry = TerminologyEntry.builder()
                    .sourceTerm(sourceTerm)
                    .targetTerm(targetTerm)
                    .sourceLanguage(sourceLanguage)
                    .targetLanguage(targetLanguage)
                    .category(parsedCategory)
                    .domain(domain)
                    .notes(definition)
                    .createdBy(createdBy)
                    .userId(userId != null ? userId : 0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return terminologyEntryRepository.save(entry);
        } catch (Exception e) {
            log.error("创建术语条目失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<TerminologyEntry> batchCreateTerminologyEntries(List<TerminologyEntry> entries) throws Exception {
        try {
            log.info("批量创建术语条目，数量: {}", entries.size());

            List<TerminologyEntry> savedEntries = new ArrayList<>();

            for (TerminologyEntry entry : entries) {
                Optional<TerminologyEntry> existing = terminologyEntryRepository
                        .findBySourceTermAndTargetTermAndSourceLanguageAndTargetLanguageAndUserId(
                                entry.getSourceTerm(),
                                entry.getTargetTerm(),
                                entry.getSourceLanguage(),
                                entry.getTargetLanguage(),
                                entry.getUserId()
                        );

                if (existing.isPresent()) {
                    log.warn("术语条目已存在，跳过: {}", entry.getSourceTerm());
                    continue;
                }

                entry.setCreatedAt(LocalDateTime.now());
                entry.setUpdatedAt(LocalDateTime.now());
                savedEntries.add(terminologyEntryRepository.save(entry));
            }

            return savedEntries;
        } catch (Exception e) {
            log.error("批量创建术语条目失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Page<TerminologyEntry> getTerminologyEntries(String sourceLanguage, String targetLanguage, String category, String domain, String createdBy, Pageable pageable) throws Exception {
        try {
            log.info("获取术语条目列表");
            Long uid = safeParseUserId(createdBy);
            TerminologyEntry.TerminologyCategory parsedCategory = category != null ? TerminologyEntry.TerminologyCategory.valueOf(category) : null;

            if (uid != null && parsedCategory != null && sourceLanguage != null && targetLanguage != null) {
                return terminologyEntryRepository.findByUserIdAndCategoryAndSourceLanguageAndTargetLanguage(
                        uid, parsedCategory, sourceLanguage, targetLanguage, pageable);
            } else if (uid != null && parsedCategory != null) {
                return terminologyEntryRepository.findByUserIdAndCategory(uid, parsedCategory, pageable);
            } else if (uid != null && sourceLanguage != null && targetLanguage != null) {
                return terminologyEntryRepository.findByUserIdAndSourceLanguageAndTargetLanguage(
                        uid, sourceLanguage, targetLanguage, pageable);
            } else if (uid != null) {
                return terminologyEntryRepository.findByUserId(uid, pageable);
            } else if (createdBy != null && (parsedCategory == null && sourceLanguage == null && targetLanguage == null)) {
                return terminologyEntryRepository.findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(createdBy, pageable);
            } else if (sourceLanguage != null && targetLanguage != null) {
                return terminologyEntryRepository.findBySourceLanguageAndTargetLanguageAndIsActiveTrue(
                        sourceLanguage, targetLanguage, pageable);
            } else if (parsedCategory != null) {
                return terminologyEntryRepository.findByCategoryAndIsActiveTrueOrderByUsageCountDesc(
                        parsedCategory, pageable);
            } else if (domain != null) {
                return terminologyEntryRepository.findByDomainAndIsActiveTrueOrderByUsageCountDesc(
                        domain, pageable);
            } else {
                return terminologyEntryRepository.findAll(pageable);
            }
        } catch (Exception e) {
            log.error("获取术语条目列表失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public TerminologyEntry getTerminologyEntryById(Long id) throws Exception {
        try {
            return terminologyEntryRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.error("获取术语条目失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public TerminologyEntry updateTerminologyEntry(Long id, String sourceTerm, String targetTerm, String category, String domain, String definition, String context) throws Exception {
        log.info("更新术语条目: {}", id);

        TerminologyEntry existing = terminologyEntryRepository.findById(id)
                .orElseThrow(() -> new Exception("术语条目不存在"));

        existing.setSourceTerm(sourceTerm);
        existing.setTargetTerm(targetTerm);
        TerminologyEntry.TerminologyCategory parsedCategory;
        try {
            parsedCategory = TerminologyEntry.TerminologyCategory.valueOf(category);
        } catch (IllegalArgumentException ex) {
            parsedCategory = TerminologyEntry.TerminologyCategory.GENERAL;
        }
        existing.setCategory(parsedCategory);
        existing.setDomain(domain);
        existing.setNotes(definition);
        existing.setUpdatedAt(LocalDateTime.now());

        return terminologyEntryRepository.save(existing);
    }

    @Override
    public boolean deleteTerminologyEntry(Long id) throws Exception {
        try {
            log.info("删除术语条目: {}", id);

            if (!terminologyEntryRepository.existsById(id)) {
                return false;
            }

            terminologyEntryRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            log.error("删除术语条目失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public int batchDeleteTerminologyEntries(List<Long> ids) throws Exception {
        try {
            log.info("批量删除术语条目: {}", ids);

            int deletedCount = 0;
            for (Long id : ids) {
                if (terminologyEntryRepository.existsById(id)) {
                    terminologyEntryRepository.deleteById(id);
                    deletedCount++;
                }
            }

            return deletedCount;
        } catch (Exception e) {
            log.error("批量删除术语条目失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Safely parses a user ID string to a Long.
     *
     * @param userIdStr the user ID string
     * @return the user ID as a Long, or null if parsing fails
     */
    private Long safeParseUserId(String userIdStr) {
        if (userIdStr == null) {
            return null;
        }
        try {
            return Long.valueOf(userIdStr.trim());
        } catch (Exception e) {
            return null;
        }
    }
}