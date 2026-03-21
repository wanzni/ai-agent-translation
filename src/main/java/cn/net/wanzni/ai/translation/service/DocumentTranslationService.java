package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.dto.DocumentProgressResponse;
import cn.net.wanzni.ai.translation.dto.DocumentStatisticsResponse;
import cn.net.wanzni.ai.translation.dto.DocumentTypeResponse;
import cn.net.wanzni.ai.translation.dto.DocumentTranslationStatisticsDTO;
import cn.net.wanzni.ai.translation.dto.QualityAssessmentResponse;
import cn.net.wanzni.ai.translation.dto.TranslationEstimateDTO;
import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文档翻译服务接口，定义文档翻译相关的核心业务功能，包括文档上传、翻译处理、进度跟踪、结果下载等功能。
 */
public interface DocumentTranslationService {

    /**
     * 上传并开始翻译文档。
     *
     * @param file 上传的文档文件
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     * @param translationType 翻译类型
     * @param translationEngine 翻译引擎
     * @param useTerminology 是否使用术语库
     * @param priority 优先级
     * @return 文档翻译任务信息
     * @throws Exception 翻译过程中的异常
     */
    DocumentTranslation uploadAndTranslate(
            MultipartFile file,
            String sourceLanguage,
            String targetLanguage,
            String translationType,
            String translationEngine,
            Boolean useTerminology,
            Integer priority
    ) throws Exception;

    /**
     * 获取文档翻译任务列表。
     *
     * @param status 任务状态（可选）
     * @param pageable 分页参数
     * @return 文档翻译任务分页列表
     * @throws Exception 查询过程中的异常
     */
    Page<DocumentTranslation> getDocumentTranslations(
            String status,
            Pageable pageable
    ) throws Exception;

    /**
     * 根据ID获取文档翻译任务详情。
     *
     * @param id 任务ID
     * @return 文档翻译任务详情，不存在则返回null
     * @throws Exception 查询过程中的异常
     */
    DocumentTranslation getDocumentTranslationById(Long id) throws Exception;

    /**
     * 获取文档翻译进度。
     *
     * @param id 任务ID
     * @return 文档翻译进度响应
     * @throws Exception 查询过程中的异常
     */
    DocumentProgressResponse getTranslationProgressResponse(Long id) throws Exception;

    /**
     * 下载翻译后的文档。
     *
     * @param id 任务ID
     * @return 文档字节数组，如果文档不存在或未完成则返回null
     * @throws Exception 下载过程中的异常
     */
    byte[] downloadTranslatedDocument(Long id) throws Exception;

    /**
     * 取消文档翻译任务。
     *
     * @param id 任务ID
     * @return 是否成功取消
     * @throws Exception 取消过程中的异常
     */
    boolean cancelTranslation(Long id) throws Exception;

    /**
     * 重新翻译文档。
     *
     * @param id 原任务ID
     * @param engine 新的翻译引擎（可选）
     * @return 新的文档翻译任务
     * @throws Exception 重新翻译过程中的异常
     */
    DocumentTranslation retranslateDocument(Long id, String engine) throws Exception;

    /**
     * 开始处理已上传的文档翻译任务（支持覆盖目标语言与翻译模式）。
     *
     * @param id 任务ID
     * @param engine 翻译引擎（可选）
     * @param targetLanguage 目标语言（可选）
     * @param translationType 翻译模式（可选）
     * @return 更新后的文档翻译任务
     * @throws Exception 处理过程中的异常
     */
    DocumentTranslation startTranslation(Long id, String engine, String targetLanguage, String translationType) throws Exception;

    /**
     * 删除文档翻译任务。
     *
     * @param id 任务ID
     * @return 是否成功删除
     * @throws Exception 删除过程中的异常
     */
    boolean deleteDocumentTranslation(Long id) throws Exception;

    /**
     * 获取支持的文档类型列表。
     *
     * @return 支持的文档类型响应列表
     * @throws Exception 查询过程中的异常
     */
    List<DocumentTypeResponse> getSupportedDocumentTypesResponse() throws Exception;

    /**
     * 获取文档翻译统计信息。
     *
     * @param userId 用户ID
     * @return 文档翻译统计信息
     * @throws Exception 查询过程中的异常
     */
    DocumentTranslationStatisticsDTO getDocumentTranslationStatistics(Long userId) throws Exception;

    /**
     * 获取文档翻译统计信息。
     *
     * @param userId 用户ID（可选，为空则获取全局统计）
     * @return 文档翻译统计响应
     * @throws Exception 统计过程中的异常
     */
    DocumentStatisticsResponse getDocumentTranslationStatisticsResponse(Long userId) throws Exception;

    /**
     * 批量删除文档翻译任务。
     *
     * @param ids 任务ID列表
     * @return 成功删除的任务数量
     * @throws Exception 删除过程中的异常
     */
    int batchDeleteDocumentTranslations(List<Long> ids) throws Exception;

    /**
     * 搜索文档翻译任务。
     *
     * @param keyword 搜索关键词（文件名）
     * @param fileType 文件类型（可选）
     * @param status 任务状态（可选）
     * @param pageable 分页参数
     * @return 搜索结果分页列表
     * @throws Exception 搜索过程中的异常
     */
    Page<DocumentTranslation> searchDocumentTranslations(
            String keyword,
            String fileType,
            String status,
            Pageable pageable
    ) throws Exception;

    /**
     * 获取文档翻译质量评估。
     *
     * @param id 任务ID
     * @return 质量评估结果
     */
    QualityAssessmentResponse getDocumentQualityAssessment(Long id);

    /**
     * 验证文档格式是否支持。
     *
     * @param filename 文件名
     * @param contentType 文件MIME类型
     * @return 是否支持该文档格式
     */
    boolean isDocumentFormatSupported(String filename, String contentType);

    /**
     * 获取文档翻译预估信息。
     *
     * @param file 文档文件
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 预估信息
     * @throws Exception 预估过程中的异常
     */
    TranslationEstimateDTO getTranslationEstimate(
            MultipartFile file,
            String sourceLanguage,
            String targetLanguage
    ) throws Exception;

    /**
     * 更新文档翻译任务状态。
     *
     * @param id 任务ID
     * @param status 新状态
     * @param progress 进度百分比（可选）
     * @param message 状态消息（可选）
     * @return 是否成功更新
     * @throws Exception 更新过程中的异常
     */
    boolean updateTranslationStatus(Long id, String status, Integer progress, String message) throws Exception;

    /**
     * 获取用户的文档翻译配额信息。
     *
     * @return 配额信息
     * @throws Exception 查询过程中的异常
     */
    Map<String, Object> getUserDocumentQuota() throws Exception;

    /**
     * 清理过期的文档翻译任务。
     *
     * @param daysToKeep 保留天数
     * @return 被清理的记录数
     * @throws Exception 如果清理过程中发生错误
     */
    long cleanupExpiredTranslations(int daysToKeep) throws Exception;
}