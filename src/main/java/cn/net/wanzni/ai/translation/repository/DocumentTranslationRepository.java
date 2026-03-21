package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.DocumentTranslation;
import cn.net.wanzni.ai.translation.enums.DocumentTypeEnum;
import cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档翻译数据访问接口
 * 
 * 提供文档翻译任务的CRUD操作和自定义查询方法
 * 
 * @version 1.0.0
 */
@Repository
public interface DocumentTranslationRepository extends JpaRepository<DocumentTranslation, Long> {

    /**
     * 根据用户ID查找文档翻译任务
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据状态查找文档翻译任务
     * 
     * @param status 处理状态
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByStatusOrderByCreatedAtDesc(
            ProcessingStatusEnum status, Pageable pageable);

    /**
     * 根据用户ID和状态查找文档翻译任务
     * 
     * @param userId 用户ID
     * @param status 处理状态
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, ProcessingStatusEnum status, Pageable pageable);

    /**
     * 根据文件类型查找文档翻译任务
     * 
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByFileTypeOrderByCreatedAtDesc(
            DocumentTypeEnum fileType, Pageable pageable);

    /**
     * 根据语言对查找文档翻译任务
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findBySourceLanguageAndTargetLanguageOrderByCreatedAtDesc(
            String sourceLanguage, String targetLanguage, Pageable pageable);

    /**
     * 根据时间范围查找文档翻译任务
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据文件名搜索文档翻译任务
     * 
     * @param filename 文件名关键词
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    @Query("SELECT dt FROM DocumentTranslation dt WHERE dt.originalFilename LIKE %:filename% ORDER BY dt.createdAt DESC")
    Page<DocumentTranslation> findByFilenameContaining(@Param("filename") String filename, Pageable pageable);

    /**
     * 查找正在处理的文档翻译任务
     * 
     * @return 正在处理的任务列表
     */
    @Query("SELECT dt FROM DocumentTranslation dt WHERE dt.status IN :statuses ORDER BY dt.createdAt ASC")
    List<DocumentTranslation> findProcessingTasks(@Param("statuses") List<ProcessingStatusEnum> statuses);

    /**
     * 查找用户正在处理的文档翻译任务
     * 
     * @param userId 用户ID
     * @return 正在处理的任务列表
     */
    @Query("SELECT dt FROM DocumentTranslation dt WHERE dt.userId = :userId AND dt.status IN :statuses ORDER BY dt.createdAt ASC")
    List<DocumentTranslation> findProcessingTasksByUserId(@Param("userId") Long userId, @Param("statuses") List<ProcessingStatusEnum> statuses);

    /**
     * 统计用户的文档翻译次数
     * 
     * @param userId 用户ID
     * @return 翻译次数
     */
    long countByUserId(Long userId);

    /**
     * 根据状态统计数量
     * 
     * @param status 处理状态
     * @return 数量
     */
    long countByStatus(ProcessingStatusEnum status);

    /**
     * 统计各状态的任务数量
     * 
     * @return 状态统计结果
     */
    @Query("SELECT dt.status, COUNT(dt) FROM DocumentTranslation dt GROUP BY dt.status")
    List<Object[]> countByStatus();

    /**
     * 统计各文件类型的翻译次数
     * 
     * @return 文件类型统计结果
     */
    @Query("SELECT dt.fileType, COUNT(dt) FROM DocumentTranslation dt GROUP BY dt.fileType ORDER BY COUNT(dt) DESC")
    List<Object[]> countByFileType();

    /**
     * 统计各语言对的文档翻译次数
     * 
     * @return 语言对统计结果
     */
    @Query("SELECT dt.sourceLanguage, dt.targetLanguage, COUNT(dt) FROM DocumentTranslation dt GROUP BY dt.sourceLanguage, dt.targetLanguage ORDER BY COUNT(dt) DESC")
    List<Object[]> countByLanguagePair();

    /**
     * 获取平均处理时间
     * 
     * @return 平均处理时间（毫秒）
     */
    @Query("SELECT AVG(dt.processingTime) FROM DocumentTranslation dt WHERE dt.processingTime IS NOT NULL AND dt.status = :status")
    Optional<Double> getAverageProcessingTime(@Param("status") ProcessingStatusEnum status);

    /**
     * 获取平均质量评分
     * 
     * @return 平均质量评分
     */
    @Query("SELECT AVG(dt.qualityScore) FROM DocumentTranslation dt WHERE dt.qualityScore IS NOT NULL")
    Optional<Double> getAverageQualityScore();

    /**
     * 查找高质量翻译文档（质量评分大于等于指定分数）
     * 
     * @param minScore 最低质量评分
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByQualityScoreGreaterThanEqualOrderByQualityScoreDesc(
            Integer minScore, Pageable pageable);

    /**
     * 查找最近完成的文档翻译任务
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 最近完成的任务列表
     */
    @Query("SELECT dt FROM DocumentTranslation dt WHERE dt.userId = :userId AND dt.status = :status ORDER BY dt.completedAt DESC")
    List<DocumentTranslation> findRecentCompletedTasks(@Param("userId") Long userId, @Param("status") ProcessingStatusEnum status, Pageable pageable);

    /**
     * 查找大文件翻译任务（文件大小大于指定值）
     * 
     * @param minSize 最小文件大小（字节）
     * @param pageable 分页参数
     * @return 大文件翻译任务分页结果
     */
    Page<DocumentTranslation> findByFileSizeGreaterThanOrderByFileSizeDesc(Long minSize, Pageable pageable);

    /**
     * 统计指定时间范围内的文档翻译次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 翻译次数
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计已完成任务的总下载次数
     * 
     * @return 总下载次数
     */
    @Query("SELECT SUM(dt.downloadCount) FROM DocumentTranslation dt WHERE dt.status = cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.COMPLETED")
    Optional<Long> getTotalDownloadCount();

    /**
     * 删除指定时间之前的已完成任务
     * 
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    long deleteByStatusAndCompletedAtBefore(ProcessingStatusEnum status, LocalDateTime beforeTime);

    /**
     * 根据文件名模糊查询（忽略大小写）
     * 
     * @param filename 文件名关键字
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
            String filename, Pageable pageable);

    /**
     * 根据文件类型和文件名模糊查询（忽略大小写）
     * 
     * @param fileType 文件类型
     * @param filename 文件名关键字
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByFileTypeAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
            DocumentTypeEnum fileType, String filename, Pageable pageable);

    /**
     * 根据用户ID和文件名模糊查询（忽略大小写）
     * 
     * @param userId 用户ID
     * @param filename 文件名关键字
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByUserIdAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long userId, String filename, Pageable pageable);

    /**
     * 根据用户ID、文件类型和文件名模糊查询（忽略大小写）
     * 
     * @param userId 用户ID
     * @param fileType 文件类型
     * @param filename 文件名关键字
     * @param pageable 分页参数
     * @return 文档翻译任务分页结果
     */
    Page<DocumentTranslation> findByUserIdAndFileTypeAndOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
            Long userId, DocumentTypeEnum fileType, String filename, Pageable pageable);

    /**
     * 获取所有语言对统计
     * 
     * @return 语言对统计结果
     */
    @Query("SELECT dt.sourceLanguage, dt.targetLanguage, COUNT(dt) FROM DocumentTranslation dt GROUP BY dt.sourceLanguage, dt.targetLanguage ORDER BY COUNT(dt) DESC")
    List<Object[]> getAllLanguagePairCounts();

    /**
     * 统计所有字符数
     * 
     * @return 字符数总和
     */
    @Query("SELECT SUM(dt.characterCount) FROM DocumentTranslation dt WHERE dt.status = cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.COMPLETED")
    Optional<Long> sumAllCharacters();

    /**
     * 获取所有文件类型统计
     * 
     * @return 文件类型统计结果
     */
    @Query("SELECT dt.fileType, COUNT(dt) FROM DocumentTranslation dt GROUP BY dt.fileType ORDER BY COUNT(dt) DESC")
    List<Object[]> getAllFileTypeCounts();

    /**
     * 查找所有文档翻译任务，按创建时间倒序排列
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<DocumentTranslation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据用户ID和状态统计数量
     * 
     * @param userId 用户ID
     * @param status 处理状态
     * @return 数量
     */
    long countByUserIdAndStatus(Long userId, ProcessingStatusEnum status);

    /**
     * 根据用户ID获取平均处理时间
     * 
     * @param userId 用户ID
     * @return 平均处理时间
     */
    @Query("SELECT AVG(dt.processingTime) FROM DocumentTranslation dt WHERE dt.userId = :userId AND dt.processingTime IS NOT NULL AND dt.status = cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.COMPLETED")
    Optional<Double> getAverageProcessingTimeByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取文件类型统计
     * 
     * @param userId 用户ID
     * @return 文件类型统计结果
     */
    @Query("SELECT dt.fileType, COUNT(dt) FROM DocumentTranslation dt WHERE dt.userId = :userId GROUP BY dt.fileType ORDER BY COUNT(dt) DESC")
    List<Object[]> getFileTypeCountsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取语言对统计
     * 
     * @param userId 用户ID
     * @return 语言对统计结果
     */
    @Query("SELECT dt.sourceLanguage, dt.targetLanguage, COUNT(dt) FROM DocumentTranslation dt WHERE dt.userId = :userId GROUP BY dt.sourceLanguage, dt.targetLanguage ORDER BY COUNT(dt) DESC")
    List<Object[]> getLanguagePairCountsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID统计字符数
     * 
     * @param userId 用户ID
     * @return 字符数总和
     */
    @Query("SELECT SUM(dt.characterCount) FROM DocumentTranslation dt WHERE dt.userId = :userId AND dt.status = cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.COMPLETED")
    Optional<Long> sumCharactersByUserId(@Param("userId") Long userId);

    /**
     * 查找过期的翻译任务
     * 
     * @param beforeTime 过期时间点
     * @return 过期的翻译任务列表
     */
    @Query("SELECT dt FROM DocumentTranslation dt WHERE dt.status IN (cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.PENDING, cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.PROCESSING) AND dt.createdAt < :beforeTime")
    List<DocumentTranslation> findExpiredTranslations(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查找长时间未完成的任务（可能需要重新处理）
     * 
     * @param beforeTime 时间阈值
     * @return 长时间未完成的任务列表
     */
    @Query("SELECT dt FROM DocumentTranslation dt WHERE dt.status IN (cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.PENDING, cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum.PROCESSING) AND dt.createdAt < :beforeTime")
    List<DocumentTranslation> findStuckTasks(@Param("beforeTime") LocalDateTime beforeTime);

    List<DocumentTranslation> findAllByCreatedAtBeforeAndStatus(LocalDateTime localDateTime, ProcessingStatusEnum uploading);
}