package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 术语库搜索服务接口
 *
 * @version 1.0.0
 */
public interface TerminologySearchService {

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
    List<Map<String, Object>> searchTermTranslations(
            String sourceTerm,
            String sourceLanguage,
            String targetLanguage,
            Boolean exactMatch
    ) throws Exception;

    /**
     * 批量搜索术语翻译
     *
     * @param sourceTerms 源术语列表
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 术语翻译映射
     * @throws Exception 搜索过程中的异常
     */
    Map<String, List<Map<String, Object>>> batchSearchTermTranslations(
            List<String> sourceTerms,
            String sourceLanguage,
            String targetLanguage
    ) throws Exception;

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
    Page<TerminologyEntry> searchTerminologyEntries(
            String keyword,
            String sourceLanguage,
            String targetLanguage,
            String category,
            String domain,
            String createdBy,
            Pageable pageable
    ) throws Exception;

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
    List<Map<String, Object>> findSimilarTerms(
            String sourceTerm,
            String sourceLanguage,
            String targetLanguage,
            Double similarity
    ) throws Exception;

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
    List<Map<String, Object>> getTermTranslationSuggestions(
            String sourceTerm,
            String sourceLanguage,
            String targetLanguage,
            String context
    ) throws Exception;
}