package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.TranslationRecord;
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
 * 翻译记录数据访问接口
 * 
 * 提供翻译记录的CRUD操作和自定义查询方法
 * 
 * @version 1.0.0
 */
@Repository
public interface TranslationRecordRepository extends JpaRepository<TranslationRecord, Long> {

    /**
     * 根据用户ID查找翻译记录
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据翻译类型查找记录
     * 
     * @param translationType 翻译类型
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findByTranslationTypeOrderByCreatedAtDesc(
            TranslationRecord.TranslationType translationType, Pageable pageable);

    /**
     * 根据语言对查找翻译记录
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findBySourceLanguageAndTargetLanguageOrderByCreatedAtDesc(
            String sourceLanguage, String targetLanguage, Pageable pageable);

    /**
     * 根据用户ID和语言对查找翻译记录
     * 
     * @param userId 用户ID
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findByUserIdAndSourceLanguageAndTargetLanguageOrderByCreatedAtDesc(
            Long userId, String sourceLanguage, String targetLanguage, Pageable pageable);

    /**
     * 根据时间范围查找翻译记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查找高质量翻译记录（质量评分大于等于指定分数）
     * 
     * @param minScore 最低质量评分
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findByQualityScoreGreaterThanEqualOrderByQualityScoreDesc(
            Integer minScore, Pageable pageable);

    /**
     * 根据源文本内容搜索翻译记录
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    @Query("SELECT tr FROM TranslationRecord tr WHERE tr.sourceText LIKE %:keyword% OR tr.translatedText LIKE %:keyword% ORDER BY tr.createdAt DESC")
    Page<TranslationRecord> findByTextContentContaining(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据文本内容和用户ID搜索翻译记录
     * 
     * @param textContent 文本内容
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT tr FROM TranslationRecord tr WHERE tr.userId = :userId AND (tr.sourceText LIKE %:textContent% OR tr.translatedText LIKE %:textContent%) ORDER BY tr.createdAt DESC")
    Page<TranslationRecord> searchByTextContentAndUserId(@Param("textContent") String textContent, @Param("userId") Long userId, Pageable pageable);

    /**
     * 根据文本内容搜索翻译记录
     * 
     * @param textContent 文本内容
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT tr FROM TranslationRecord tr WHERE tr.sourceText LIKE %:textContent% OR tr.translatedText LIKE %:textContent% ORDER BY tr.createdAt DESC")
    Page<TranslationRecord> searchByTextContent(@Param("textContent") String textContent, Pageable pageable);

    /**
     * 统计用户的翻译次数
     * 
     * @param userId 用户ID
     * @return 翻译次数
     */
    long countByUserId(Long userId);

    /**
     * 统计指定时间范围内的翻译次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 翻译次数
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计各翻译类型的使用次数
     * 
     * @return 翻译类型统计结果
     */
    @Query("SELECT tr.translationType, COUNT(tr) FROM TranslationRecord tr GROUP BY tr.translationType")
    List<Object[]> countByTranslationType();

    /**
     * 统计各语言对的翻译次数
     * 
     * @return 语言对统计结果
     */
    @Query("SELECT tr.sourceLanguage, tr.targetLanguage, COUNT(tr) FROM TranslationRecord tr GROUP BY tr.sourceLanguage, tr.targetLanguage ORDER BY COUNT(tr) DESC")
    List<Object[]> countByLanguagePair();

    /**
     * 获取平均质量评分
     * 
     * @return 平均质量评分
     */
    @Query("SELECT AVG(tr.qualityScore) FROM TranslationRecord tr WHERE tr.qualityScore IS NOT NULL")
    Optional<Double> getAverageQualityScore();

    /**
     * 获取用户的平均质量评分
     * 
     * @param userId 用户ID
     * @return 平均质量评分
     */
    @Query("SELECT AVG(tr.qualityScore) FROM TranslationRecord tr WHERE tr.userId = :userId AND tr.qualityScore IS NOT NULL")
    Optional<Double> getAverageQualityScoreByUserId(@Param("userId") Long userId);

    /**
     * 查找最近的翻译记录
     * 
     * @param userId 用户ID
     * @return 最近的翻译记录列表
     */
    @Query("SELECT tr FROM TranslationRecord tr WHERE tr.userId = :userId ORDER BY tr.createdAt DESC")
    List<TranslationRecord> findRecentTranslations(@Param("userId") Long userId, Pageable pageable);

    /**
     * 删除指定时间之前的翻译记录
     * 
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    long deleteByCreatedAtBefore(LocalDateTime beforeTime);

    /**
     * 统计所有字符数
     * 
     * @return 字符数总和
     */
    @Query("SELECT SUM(tr.characterCount) FROM TranslationRecord tr")
    Optional<Long> sumAllCharacters();

    /**
     * 获取所有语言对统计
     * 
     * @return 语言对统计结果
     */
    @Query("SELECT tr.sourceLanguage, tr.targetLanguage, COUNT(tr) FROM TranslationRecord tr GROUP BY tr.sourceLanguage, tr.targetLanguage ORDER BY COUNT(tr) DESC")
    List<Object[]> getAllLanguagePairCounts();

    /**
     * 获取所有翻译类型统计
     * 
     * @return 翻译类型统计结果
     */
    @Query("SELECT tr.translationType, COUNT(tr) FROM TranslationRecord tr GROUP BY tr.translationType ORDER BY COUNT(tr) DESC")
    List<Object[]> getAllTranslationTypeCounts();

    /**
     * 查找所有翻译记录并按创建时间倒序排列
     * 
     * @param pageable 分页参数
     * @return 翻译记录分页结果
     */
    Page<TranslationRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据用户ID统计字符数
     * 
     * @param userId 用户ID
     * @return 字符数总和
     */
    @Query("SELECT SUM(tr.characterCount) FROM TranslationRecord tr WHERE tr.userId = :userId")
    Optional<Long> sumCharactersByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取语言对统计
     * 
     * @param userId 用户ID
     * @return 语言对统计结果
     */
    @Query("SELECT tr.sourceLanguage, tr.targetLanguage, COUNT(tr) FROM TranslationRecord tr WHERE tr.userId = :userId GROUP BY tr.sourceLanguage, tr.targetLanguage ORDER BY COUNT(tr) DESC")
    List<Object[]> getLanguagePairCountsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取翻译类型统计
     * 
     * @param userId 用户ID
     * @return 翻译类型统计结果
     */
    @Query("SELECT tr.translationType, COUNT(tr) FROM TranslationRecord tr WHERE tr.userId = :userId GROUP BY tr.translationType ORDER BY COUNT(tr) DESC")
    List<Object[]> getTranslationTypeCountsByUserId(@Param("userId") Long userId);
}