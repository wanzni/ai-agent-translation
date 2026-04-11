package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.CategoryCountDTO;
import cn.net.wanzni.ai.translation.dto.LanguagePairCountDTO;
import cn.net.wanzni.ai.translation.dto.TerminologyStatsResponse;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.service.TerminologyStatisticsService;
import cn.net.wanzni.ai.translation.util.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologyStatisticsServiceImpl implements TerminologyStatisticsService {

    private final TerminologyEntryRepository terminologyEntryRepository;

    @Override
    public Map<String, Object> getTerminologyStatistics(String createdBy) throws Exception {
        try {
            log.info("获取术语统计信息: userId={}", createdBy);

            Map<String, Object> statistics = new HashMap<>();
            Long uid = safeParseUserId(createdBy);
            if (uid == null) {
                String currentUserId = UserContextUtils.getCurrentValidUserId();
                uid = safeParseUserId(currentUserId);
            }

            if (uid == null) {
                statistics.put("totalEntries", 0L);
                statistics.put("categoryCounts", Collections.emptyList());
                statistics.put("languagePairCounts", Collections.emptyList());
                return statistics;
            }

            List<TerminologyEntry> entries = terminologyEntryRepository.findByUserId(uid, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(entry -> !Boolean.FALSE.equals(entry.getIsActive()))
                    .collect(Collectors.toList());

            statistics.put("totalEntries", (long) entries.size());
            statistics.put("categoryCounts", aggregateCategoryCounts(entries));
            statistics.put("languagePairCounts", aggregateLanguagePairCounts(entries));
            return statistics;
        } catch (Exception e) {
            log.error("获取术语统计信息失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public TerminologyStatsResponse getTerminologyStatisticsResponse(String createdBy) throws Exception {
        Map<String, Object> statsMap = getTerminologyStatistics(createdBy);

        long totalEntries = 0L;
        Object totalEntriesObj = statsMap.get("totalEntries");
        if (totalEntriesObj instanceof Number) {
            totalEntries = ((Number) totalEntriesObj).longValue();
        }

        List<CategoryCountDTO> categoryCounts = new ArrayList<>();
        Object categoryCountsObj = statsMap.get("categoryCounts");
        if (categoryCountsObj instanceof List<?>) {
            for (Object row : (List<?>) categoryCountsObj) {
                if (row instanceof Object[]) {
                    Object[] arr = (Object[]) row;
                    String cat = String.valueOf(arr[0]);
                    long cnt = arr[1] instanceof Number ? ((Number) arr[1]).longValue() : 0L;
                    categoryCounts.add(CategoryCountDTO.builder().category(cat).count(cnt).build());
                }
            }
        }

        List<LanguagePairCountDTO> languagePairCounts = new ArrayList<>();
        Object lpCountsObj = statsMap.get("languagePairCounts");
        if (lpCountsObj instanceof List<?>) {
            for (Object row : (List<?>) lpCountsObj) {
                if (row instanceof Object[]) {
                    Object[] arr = (Object[]) row;
                    String src = String.valueOf(arr[0]);
                    String tgt = String.valueOf(arr[1]);
                    long cnt = arr[2] instanceof Number ? ((Number) arr[2]).longValue() : 0L;
                    languagePairCounts.add(LanguagePairCountDTO.builder()
                            .sourceLanguage(src)
                            .targetLanguage(tgt)
                            .count(cnt)
                            .build());
                }
            }
        }

        return TerminologyStatsResponse.builder()
                .totalEntries(totalEntries)
                .categoryCounts(categoryCounts)
                .languagePairCounts(languagePairCounts)
                .totalTerms(totalEntries)
                .categoryCount(categoryCounts.size())
                .build();
    }

    @Override
    public List<Map<String, Object>> getTermUsageStatistics(String sourceLanguage, String targetLanguage, Integer limit) throws Exception {
        try {
            log.info("获取术语使用频率统计");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取术语使用频率统计失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<String> getTerminologyCategories() throws Exception {
        try {
            log.info("获取术语分类列表");
            return terminologyEntryRepository.findDistinctCategories().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取术语分类列表失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public List<String> getDomainTags() throws Exception {
        try {
            log.info("获取领域标签列表");
            return Arrays.asList("技术", "商务", "医学", "法律", "金融", "教育", "科学", "通用");
        } catch (Exception e) {
            log.error("获取领域标签列表失败: {}", e.getMessage());
            throw e;
        }
    }

    private List<Object[]> aggregateCategoryCounts(List<TerminologyEntry> entries) {
        Map<String, Long> counts = entries.stream()
                .filter(entry -> entry.getCategory() != null)
                .collect(Collectors.groupingBy(entry -> entry.getCategory().name(), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .collect(Collectors.toList());
    }

    private List<Object[]> aggregateLanguagePairCounts(List<TerminologyEntry> entries) {
        Map<String, Long> counts = entries.stream()
                .filter(entry -> entry.getSourceLanguage() != null && entry.getTargetLanguage() != null)
                .collect(Collectors.groupingBy(
                        entry -> entry.getSourceLanguage() + "\u0000" + entry.getTargetLanguage(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        return counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .map(entry -> {
                    String[] parts = entry.getKey().split("\u0000", 2);
                    return new Object[]{parts[0], parts[1], entry.getValue()};
                })
                .collect(Collectors.toList());
    }

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