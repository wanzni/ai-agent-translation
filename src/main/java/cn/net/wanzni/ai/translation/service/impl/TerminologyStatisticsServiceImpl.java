package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.CategoryCountDTO;
import cn.net.wanzni.ai.translation.dto.LanguagePairCountDTO;
import cn.net.wanzni.ai.translation.dto.TerminologyStatsResponse;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.service.TerminologyStatisticsService;
import cn.net.wanzni.ai.translation.util.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 术语库统计服务实现类
 *
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologyStatisticsServiceImpl implements TerminologyStatisticsService {

    private final TerminologyEntryRepository terminologyEntryRepository;

    /**
     * 获取术语库统计信息
     *
     * @param createdBy 创建者ID（可选，为空则获取全局统计）
     * @return 统计信息Map，包含术语数量、语言对分布、分类统计等
     * @throws Exception 统计过程中的异常
     */
    @Override
    public Map<String, Object> getTerminologyStatistics(String createdBy) throws Exception {
        try {
            log.info("获取术语统计信息: userId={}", createdBy);

            Map<String, Object> statistics = new HashMap<>();
            // 统一以当前登录用户为优先；若未提供 createdBy 则尝试从请求上下文获取
            Long uid = safeParseUserId(createdBy);
            if (uid == null) {
                String currentUserId = UserContextUtils.getCurrentValidUserId();
                uid = safeParseUserId(currentUserId);
            }

            if (uid != null) {
                statistics.put("totalEntries", terminologyEntryRepository.countByUserId(uid));
                statistics.put("categoryCounts", terminologyEntryRepository.getCategoryCountsByUserId(uid));
                statistics.put("languagePairCounts", terminologyEntryRepository.getLanguagePairCountsByUserId(uid));
            } else {
                // 未识别到有效用户，则返回空统计，避免展示全局数据
                statistics.put("totalEntries", 0L);
                statistics.put("categoryCounts", java.util.Collections.emptyList());
                statistics.put("languagePairCounts", java.util.Collections.emptyList());
            }

            return statistics;
        } catch (Exception e) {
            log.error("获取术语统计信息失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取术语库统计（DTO版本，控制器零逻辑直接返回）
     *
     * @param createdBy 创建者（可选）
     * @return 术语库统计响应体
     * @throws Exception 查询过程中的异常
     */
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

    /**
     * 获取术语使用频率统计
     *
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param limit 返回数量限制
     * @return 使用频率统计列表
     * @throws Exception 统计过程中的异常
     */
    @Override
    public List<Map<String, Object>> getTermUsageStatistics(String sourceLanguage, String targetLanguage, Integer limit) throws Exception {
        try {
            log.info("获取术语使用频率统计");

            List<Map<String, Object>> results = new ArrayList<>();
            return results;
        } catch (Exception e) {
            log.error("获取术语使用频率统计失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取术语分类列表
     *
     * @return 术语分类列表
     * @throws Exception 查询过程中的异常
     */
    @Override
    public List<String> getTerminologyCategories() throws Exception {
        try {
            log.info("获取术语分类列表");
            return terminologyEntryRepository.findDistinctCategories().stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("获取术语分类列表失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 获取领域标签列表
     *
     * @return 领域标签列表
     * @throws Exception 查询过程中的异常
     */
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

    /**
     * 安全地将用户ID字符串解析为Long类型
     *
     * @param userIdStr 用户ID字符串
     * @return 解析成功返回Long类型的用户ID，否则返回null
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