package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.TerminologyStatsResponse;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.service.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 术语库服务门面实现类
 *
 * @version 1.0.0
 */
@Service
public class TerminologyServiceImpl implements TerminologyService {

    private final TerminologyCrudService crudService;
    private final TerminologySearchService searchService;
    private final TerminologyImportExportService importExportService;
    private final TerminologyStatisticsService statisticsService;
    private final TerminologyMaintenanceService maintenanceService;

    public TerminologyServiceImpl(
            @Qualifier("terminologyCrudServiceImpl") TerminologyCrudService crudService,
            @Qualifier("terminologySearchServiceImpl") TerminologySearchService searchService,
            @Qualifier("terminologyImportExportServiceImpl") TerminologyImportExportService importExportService,
            @Qualifier("terminologyStatisticsServiceImpl") TerminologyStatisticsService statisticsService,
            @Qualifier("terminologyMaintenanceServiceImpl") TerminologyMaintenanceService maintenanceService) {
        this.crudService = crudService;
        this.searchService = searchService;
        this.importExportService = importExportService;
        this.statisticsService = statisticsService;
        this.maintenanceService = maintenanceService;
    }

    /**
     * 创建一个术语条目。
     *
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     * @param category 术语分类
     * @param domain 领域标签
     * @param definition 术语定义
     * @param context 使用上下文
     * @param createdBy 创建者ID
     * @return 创建的术语条目
     * @throws Exception 创建过程中的异常
     */
    @Override
    public TerminologyEntry createTerminologyEntry(String sourceTerm, String targetTerm, String sourceLanguage, String targetLanguage, String category, String domain, String definition, String context, String createdBy) throws Exception {
        return crudService.createTerminologyEntry(sourceTerm, targetTerm, sourceLanguage, targetLanguage, category, domain, definition, context, createdBy);
    }

    /**
     * 批量创建多个术语条目。
     *
     * @param entries 术语条目列表
     * @return 成功创建的术语条目列表
     * @throws Exception 创建过程中的异常
     */
    @Override
    public List<TerminologyEntry> batchCreateTerminologyEntries(List<TerminologyEntry> entries) throws Exception {
        return crudService.batchCreateTerminologyEntries(entries);
    }

    /**
     * 根据条件分页获取术语条目列表。
     *
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param category 分类（可选）
     * @param domain 领域（可选）
     * @param createdBy 创建者（可选）
     * @param pageable 分页参数
     * @return 术语条目分页列表
     * @throws Exception 查询过程中的异常
     */
    @Override
    public Page<TerminologyEntry> getTerminologyEntries(String sourceLanguage, String targetLanguage, String category, String domain, String createdBy, Pageable pageable) throws Exception {
        return crudService.getTerminologyEntries(sourceLanguage, targetLanguage, category, domain, createdBy, pageable);
    }

    /**
     * 根据 ID 获取单个术语条目。
     *
     * @param id 术语条目ID
     * @return 术语条目详情，不存在则返回null
     * @throws Exception 查询过程中的异常
     */
    @Override
    public TerminologyEntry getTerminologyEntryById(Long id) throws Exception {
        return crudService.getTerminologyEntryById(id);
    }

    /**
     * 更新一个现有的术语条目。
     *
     * @param id 术语条目ID
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param category 术语分类
     * @param domain 领域标签
     * @param definition 术语定义
     * @param context 使用上下文
     * @return 更新后的术语条目
     * @throws Exception 更新过程中的异常
     */
    @Override
    public TerminologyEntry updateTerminologyEntry(Long id, String sourceTerm, String targetTerm, String category, String domain, String definition, String context) throws Exception {
        return crudService.updateTerminologyEntry(id, sourceTerm, targetTerm, category, domain, definition, context);
    }

    /**
     * 根据 ID 删除一个术语条目。
     *
     * @param id 术语条目ID
     * @return 是否成功删除
     * @throws Exception 删除过程中的异常
     */
    @Override
    public boolean deleteTerminologyEntry(Long id) throws Exception {
        return crudService.deleteTerminologyEntry(id);
    }

    /**
     * 批量删除多个术语条目。
     *
     * @param ids 术语条目ID列表
     * @return 成功删除的条目数量
     * @throws Exception 删除过程中的异常
     */
    @Override
    public int batchDeleteTerminologyEntries(List<Long> ids) throws Exception {
        return crudService.batchDeleteTerminologyEntries(ids);
    }

    /**
     * 搜索术语翻译
     *
     * @param sourceTerm 源术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param exactMatch 是否精确匹配
     * @return 匹配的术语翻译列表
     * @throws Exception 搜索过程中的异常
     */
    @Override
    public List<Map<String, Object>> searchTermTranslations(String sourceTerm, String sourceLanguage, String targetLanguage, Boolean exactMatch) throws Exception {
        return searchService.searchTermTranslations(sourceTerm, sourceLanguage, targetLanguage, exactMatch);
    }

    /**
     * 批量搜索术语翻译
     *
     * @param sourceTerms 源术语列表
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 术语翻译映射
     * @throws Exception 搜索过程中的异常
     */
    @Override
    public Map<String, List<Map<String, Object>>> batchSearchTermTranslations(List<String> sourceTerms, String sourceLanguage, String targetLanguage) throws Exception {
        return searchService.batchSearchTermTranslations(sourceTerms, sourceLanguage, targetLanguage);
    }

    /**
     * 搜索术语条目
     *
     * @param keyword 搜索关键词
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param category 分类（可选）
     * @param domain 领域（可选）
     * @param createdBy 创建者（可选）
     * @param pageable 分页参数
     * @return 搜索结果分页列表
     * @throws Exception 搜索过程中的异常
     */
    @Override
    public Page<TerminologyEntry> searchTerminologyEntries(String keyword, String sourceLanguage, String targetLanguage, String category, String domain, String createdBy, Pageable pageable) throws Exception {
        return searchService.searchTerminologyEntries(keyword, sourceLanguage, targetLanguage, category, domain, createdBy, pageable);
    }

    /**
     * 查找相似术语
     *
     * @param sourceTerm 源术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param similarity 相似度阈值（0.0-1.0）
     * @return 相似术语列表
     * @throws Exception 查找过程中的异常
     */
    @Override
    public List<Map<String, Object>> findSimilarTerms(String sourceTerm, String sourceLanguage, String targetLanguage, Double similarity) throws Exception {
        return searchService.findSimilarTerms(sourceTerm, sourceLanguage, targetLanguage, similarity);
    }

    /**
     * 获取术语翻译建议
     *
     * @param sourceTerm 源术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param context 上下文（可选）
     * @return 翻译建议列表
     * @throws Exception 获取建议过程中的异常
     */
    @Override
    public List<Map<String, Object>> getTermTranslationSuggestions(String sourceTerm, String sourceLanguage, String targetLanguage, String context) throws Exception {
        return searchService.getTermTranslationSuggestions(sourceTerm, sourceLanguage, targetLanguage, context);
    }

    /**
     * 导入术语库
     *
     * @param file 术语库文件（支持CSV、Excel等格式）
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param category 默认分类
     * @param domain 默认领域
     * @param createdBy 创建者ID
     * @return 导入结果信息
     * @throws Exception 导入过程中的异常
     */
    @Override
    public Map<String, Object> importTerminology(MultipartFile file, String sourceLanguage, String targetLanguage, String category, String domain, String createdBy) throws Exception {
        return importExportService.importTerminology(file, sourceLanguage, targetLanguage, category, domain, createdBy);
    }

    /**
     * 导出术语库
     *
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param category 分类（可选）
     * @param domain 领域（可选）
     * @param format 导出格式（csv、excel）
     * @return 导出文件的字节数组
     * @throws Exception 导出过程中的异常
     */
    @Override
    public byte[] exportTerminology(String sourceLanguage, String targetLanguage, String category, String domain, String format) throws Exception {
        return importExportService.exportTerminology(sourceLanguage, targetLanguage, category, domain, format);
    }

    /**
     * 获取术语库统计信息
     *
     * @param createdBy 创建者ID（可选，为空则获取全局统计）
     * @return 统计信息Map，包含术语数量、语言对分布、分类统计等
     * @throws Exception 统计过程中的异常
     */
    @Override
    public Map<String, Object> getTerminologyStatistics(String createdBy) throws Exception {
        return statisticsService.getTerminologyStatistics(createdBy);
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
        return statisticsService.getTerminologyStatisticsResponse(createdBy);
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
        return statisticsService.getTermUsageStatistics(sourceLanguage, targetLanguage, limit);
    }

    /**
     * 获取术语分类列表
     *
     * @return 术语分类列表
     * @throws Exception 查询过程中的异常
     */
    @Override
    public List<String> getTerminologyCategories() throws Exception {
        return statisticsService.getTerminologyCategories();
    }

    /**
     * 获取领域标签列表
     *
     * @return 领域标签列表
     * @throws Exception 查询过程中的异常
     */
    @Override
    public List<String> getDomainTags() throws Exception {
        return statisticsService.getDomainTags();
    }

    /**
     * 验证术语条目
     *
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 验证结果Map，包含是否有效、错误信息、建议等
     * @throws Exception 验证过程中的异常
     */
    @Override
    public Map<String, Object> validateTerminologyEntry(String sourceTerm, String targetTerm, String sourceLanguage, String targetLanguage) throws Exception {
        return maintenanceService.validateTerminologyEntry(sourceTerm, targetTerm, sourceLanguage, targetLanguage);
    }

    /**
     * 合并重复术语
     *
     * @param primaryId 主术语ID
     * @param duplicateIds 重复术语ID列表
     * @return 合并结果信息
     * @throws Exception 合并过程中的异常
     */
    @Override
    public Map<String, Object> mergeDuplicateTerms(Long primaryId, List<Long> duplicateIds) throws Exception {
        return maintenanceService.mergeDuplicateTerms(primaryId, duplicateIds);
    }

    /**
     * 更新术语使用次数
     *
     * @param id 术语条目ID
     * @return 是否成功更新
     * @throws Exception 更新过程中的异常
     */
    @Override
    public boolean incrementTermUsage(Long id) throws Exception {
        return maintenanceService.incrementTermUsage(id);
    }

    /**
     * 批量更新术语使用次数
     *
     * @param ids 术语条目ID列表
     * @return 成功更新的条目数量
     * @throws Exception 更新过程中的异常
     */
    @Override
    public int batchIncrementTermUsage(List<Long> ids) throws Exception {
        return maintenanceService.batchIncrementTermUsage(ids);
    }
}
