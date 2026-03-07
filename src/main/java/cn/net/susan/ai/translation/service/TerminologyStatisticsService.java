package cn.net.susan.ai.translation.service;

import cn.net.susan.ai.translation.dto.TerminologyStatsResponse;

import java.util.List;
import java.util.Map;

/**
 * 术语库统计服务接口
 *
 * @author sushan
 * @version 1.0.0
 */
public interface TerminologyStatisticsService {

    /**
     * 获取术语库统计信息
     *
     * @param createdBy 创建者ID（可选，为空则获取全局统计）
     * @return 统计信息Map，包含术语数量、语言对分布、分类统计等
     * @throws Exception 统计过程中的异常
     */
    Map<String, Object> getTerminologyStatistics(String createdBy) throws Exception;

    /**
     * 获取术语库统计（DTO版本，控制器零逻辑直接返回）
     *
     * @param createdBy 创建者（可选）
     * @return 术语库统计响应体
     * @throws Exception 查询过程中的异常
     */
    TerminologyStatsResponse getTerminologyStatisticsResponse(String createdBy) throws Exception;

    /**
     * 获取术语使用频率统计
     *
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param limit 返回数量限制
     * @return 使用频率统计列表
     * @throws Exception 统计过程中的异常
     */
    List<Map<String, Object>> getTermUsageStatistics(
            String sourceLanguage,
            String targetLanguage,
            Integer limit
    ) throws Exception;

    /**
     * 获取术语分类列表
     *
     * @return 术语分类列表
     * @throws Exception 查询过程中的异常
     */
    List<String> getTerminologyCategories() throws Exception;

    /**
     * 获取领域标签列表
     *
     * @return 领域标签列表
     * @throws Exception 查询过程中的异常
     */
    List<String> getDomainTags() throws Exception;
}