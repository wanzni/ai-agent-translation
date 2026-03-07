package cn.net.susan.ai.translation.service;

import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.dto.LanguageDetectionResponse;
import cn.net.susan.ai.translation.dto.SupportedLanguageResponse;
import cn.net.susan.ai.translation.dto.TranslationStatisticsResponse;
import cn.net.susan.ai.translation.entity.TranslationRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 翻译服务接口，定义了核心翻译功能，包括文本翻译、语言检测、翻译历史管理等。
 */
public interface TranslationService {

    /**
     * 执行文本翻译。
     * 
     * @param request 翻译请求
     * @return 翻译结果
     * @throws Exception 翻译过程中发生的异常
     */
    TranslationResponse translate(TranslationRequest request) throws Exception;

    /**
     * 批量翻译多个文本。
     * 
     * @param requests 翻译请求列表
     * @return 翻译结果列表
     * @throws Exception 翻译过程中发生的异常
     */
    List<TranslationResponse> batchTranslate(List<TranslationRequest> requests) throws Exception;

    /**
     * 检测给定文本的语言。
     * 
     * @param text 待检测文本
     * @return 检测结果，包含语言代码和置信度
     * @throws Exception 检测过程中发生的异常
     */
    LanguageDetectionResponse detectLanguage(String text) throws Exception;

    /**
     * 获取所有支持的语言列表。
     * 
     * @return 语言代码和名称的映射
     * @throws Exception 获取过程中发生的异常
     */
    List<SupportedLanguageResponse> getSupportedLanguages() throws Exception;

    /**
     * 分页获取翻译历史记录。
     * 
     * @param userId 用户ID（可选）
     * @param pageable 分页参数
     * @return 翻译历史分页结果
     * @throws Exception 查询过程中发生的异常
     */
    Page<TranslationRecord> getTranslationHistory(Long userId, Pageable pageable) throws Exception;

    /**
     * 根据 ID 获取指定的翻译记录。
     * 
     * @param id 翻译记录ID
     * @return 翻译记录，如果不存在返回null
     * @throws Exception 查询过程中发生的异常
     */
    TranslationRecord getTranslationById(Long id) throws Exception;

    /**
     * 根据 ID 删除指定的翻译记录。
     * 
     * @param id 翻译记录ID
     * @return 是否删除成功
     * @throws Exception 删除过程中发生的异常
     */
    boolean deleteTranslation(Long id) throws Exception;

    /**
     * 获取指定用户的翻译统计信息。
     * 
     * @param userId 用户ID（可选）
     * @return 统计信息
     * @throws Exception 统计过程中发生的异常
     */
    TranslationStatisticsResponse getTranslationStatistics(Long userId) throws Exception;

    /**
     * 对指定的翻译记录进行重新翻译。
     * 
     * @param id 原翻译记录ID
     * @param engine 新的翻译引擎（可选）
     * @return 新的翻译结果
     * @throws Exception 翻译过程中发生的异常
     */
    TranslationResponse retranslate(Long id, String engine) throws Exception;

    /**
     * 根据文本内容搜索翻译记录。
     * 
     * @param keyword 搜索关键词
     * @param userId 用户ID（可选）
     * @param pageable 分页参数
     * @return 搜索结果
     * @throws Exception 搜索过程中发生的异常
     */
    Page<TranslationRecord> searchTranslations(String keyword, Long userId, Pageable pageable) throws Exception;

    /**
     * 获取指定翻译记录的质量评估结果。
     * 
     * @param translationId 翻译记录ID
     * @return 质量评估结果
     * @throws Exception 评估过程中发生的异常
     */
    Map<String, Object> getQualityAssessment(Long translationId) throws Exception;

    /**
     * 获取所有可用的翻译引擎列表。
     * 
     * @return 可用的翻译引擎列表
     */
    List<String> getAvailableEngines();

    /**
     * 验证指定的语言对是否受支持。
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 是否支持该语言对
     */
    boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage);

    /**
     * 从缓存中获取翻译结果。
     * 
     * @param sourceText 源文本
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 缓存的翻译结果，如果没有缓存则返回null
     */
    String getTranslationCache(String sourceText, String sourceLanguage, String targetLanguage);

    /**
     * 将翻译结果设置到缓存中。
     * 
     * @param sourceText 源文本
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param translatedText 翻译结果
     */
    void setTranslationCache(String sourceText, String sourceLanguage, String targetLanguage, String translatedText);
}