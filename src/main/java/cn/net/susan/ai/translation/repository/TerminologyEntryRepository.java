package cn.net.susan.ai.translation.repository;

import cn.net.susan.ai.translation.entity.TerminologyEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 术语库条目数据访问接口
 * 
 * 提供术语库的CRUD操作和自定义查询方法
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Repository
public interface TerminologyEntryRepository extends JpaRepository<TerminologyEntry, Long> {

    /**
     * 根据源术语和语言对查找术语条目
     * 
     * @param sourceTerm 源术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 术语条目
     */
    Optional<TerminologyEntry> findBySourceTermAndSourceLanguageAndTargetLanguage(
            String sourceTerm, String sourceLanguage, String targetLanguage);

    /**
     * 根据语言对查找所有术语条目
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findBySourceLanguageAndTargetLanguageAndIsActiveTrue(
            String sourceLanguage, String targetLanguage, Pageable pageable);

    /**
     * 根据分类查找术语条目
     * 
     * @param category 术语分类
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByCategoryAndIsActiveTrueOrderByUsageCountDesc(
            TerminologyEntry.TerminologyCategory category, Pageable pageable);

    /**
     * 根据领域查找术语条目
     * 
     * @param domain 领域标签
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByDomainAndIsActiveTrueOrderByUsageCountDesc(
            String domain, Pageable pageable);

    /**
     * 搜索术语条目（支持源术语和目标术语搜索）
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    @Query("SELECT te FROM TerminologyEntry te WHERE te.isActive = true AND " +
           "(te.sourceTerm LIKE %:keyword% OR te.targetTerm LIKE %:keyword% OR te.notes LIKE %:keyword%) " +
           "ORDER BY te.usageCount DESC")
    Page<TerminologyEntry> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据语言对和关键词搜索术语条目
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    @Query("SELECT te FROM TerminologyEntry te WHERE te.isActive = true AND " +
           "te.sourceLanguage = :sourceLanguage AND te.targetLanguage = :targetLanguage AND " +
           "(te.sourceTerm LIKE %:keyword% OR te.targetTerm LIKE %:keyword% OR te.notes LIKE %:keyword%) " +
           "ORDER BY te.usageCount DESC")
    Page<TerminologyEntry> searchByLanguagePairAndKeyword(
            @Param("sourceLanguage") String sourceLanguage,
            @Param("targetLanguage") String targetLanguage,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 查找最常用的术语条目
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param limit 限制数量
     * @return 最常用的术语条目列表
     */
    @Query("SELECT te FROM TerminologyEntry te WHERE te.isActive = true AND " +
           "te.sourceLanguage = :sourceLanguage AND te.targetLanguage = :targetLanguage " +
           "ORDER BY te.usageCount DESC")
    List<TerminologyEntry> findMostUsedTerms(
            @Param("sourceLanguage") String sourceLanguage,
            @Param("targetLanguage") String targetLanguage,
            Pageable pageable);

    /**
     * 根据创建者查找术语条目
     * 
     * @param createdBy 创建者ID
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(
            String createdBy, Pageable pageable);

    /**
     * 统计各分类的术语数量
     * 
     * @return 分类统计结果
     */
    @Query("SELECT te.category, COUNT(te) FROM TerminologyEntry te WHERE te.isActive = true GROUP BY te.category")
    List<Object[]> countByCategory();

    /**
     * 统计各领域的术语数量
     * 
     * @return 领域统计结果
     */
    @Query("SELECT te.domain, COUNT(te) FROM TerminologyEntry te WHERE te.isActive = true AND te.domain IS NOT NULL GROUP BY te.domain ORDER BY COUNT(te) DESC")
    List<Object[]> countByDomain();

    /**
     * 统计各语言对的术语数量
     * 
     * @return 语言对统计结果
     */
    @Query("SELECT te.sourceLanguage, te.targetLanguage, COUNT(te) FROM TerminologyEntry te WHERE te.isActive = true GROUP BY te.sourceLanguage, te.targetLanguage ORDER BY COUNT(te) DESC")
    List<Object[]> countByLanguagePair();

    /**
     * 查找重复的术语条目
     * 
     * @return 重复术语条目列表
     */
    @Query("SELECT te1 FROM TerminologyEntry te1 WHERE te1.isActive = true AND EXISTS " +
           "(SELECT te2 FROM TerminologyEntry te2 WHERE te2.isActive = true AND te2.id != te1.id AND " +
           "te2.sourceTerm = te1.sourceTerm AND te2.sourceLanguage = te1.sourceLanguage AND " +
           "te2.targetLanguage = te1.targetLanguage)")
    List<TerminologyEntry> findDuplicateTerms();

    /**
     * 批量查找术语翻译
     * 
     * @param sourceTerms 源术语列表
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 匹配的术语条目列表
     */
    @Query("SELECT te FROM TerminologyEntry te WHERE te.isActive = true AND " +
           "te.sourceTerm IN :sourceTerms AND te.sourceLanguage = :sourceLanguage AND te.targetLanguage = :targetLanguage")
    List<TerminologyEntry> findBySourceTermsAndLanguagePair(
            @Param("sourceTerms") List<String> sourceTerms,
            @Param("sourceLanguage") String sourceLanguage,
            @Param("targetLanguage") String targetLanguage);

    /**
     * 统计活跃术语条目总数
     * 
     * @return 活跃术语条目数量
     */
    long countByIsActiveTrue();

    /**
     * 根据语言对统计活跃术语条目数量
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 术语条目数量
     */
    long countBySourceLanguageAndTargetLanguageAndIsActiveTrue(String sourceLanguage, String targetLanguage);

    // 新增用户相关的查询方法

    /**
     * 根据用户ID查找术语条目
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID查找术语条目（不分页）
     * 
     * @param userId 用户ID
     * @return 术语条目列表
     */
    List<TerminologyEntry> findByUserId(Long userId);

    /**
     * 根据用户ID和分类查找术语条目
     * 
     * @param userId 用户ID
     * @param category 分类
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByUserIdAndCategory(Long userId, TerminologyEntry.TerminologyCategory category, Pageable pageable);

    /**
     * 根据用户ID和分类查找术语条目（不分页）
     * 
     * @param userId 用户ID
     * @param category 分类
     * @return 术语条目列表
     */
    List<TerminologyEntry> findByUserIdAndCategory(Long userId, TerminologyEntry.TerminologyCategory category);

    /**
     * 根据用户ID和语言对查找术语条目
     * 
     * @param userId 用户ID
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByUserIdAndSourceLanguageAndTargetLanguage(
            Long userId, String sourceLanguage, String targetLanguage, Pageable pageable);

    /**
     * 根据用户ID和语言对查找术语条目（不分页）
     * 
     * @param userId 用户ID
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 术语条目列表
     */
    List<TerminologyEntry> findByUserIdAndSourceLanguageAndTargetLanguage(
            Long userId, String sourceLanguage, String targetLanguage);

    /**
     * 根据用户ID、分类和语言对查找术语条目
     * 
     * @param userId 用户ID
     * @param category 分类
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    Page<TerminologyEntry> findByUserIdAndCategoryAndSourceLanguageAndTargetLanguage(
            Long userId, TerminologyEntry.TerminologyCategory category, String sourceLanguage, String targetLanguage, Pageable pageable);

    /**
     * 根据用户ID、分类和语言对查找术语条目（不分页）
     * 
     * @param userId 用户ID
     * @param category 分类
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 术语条目列表
     */
    List<TerminologyEntry> findByUserIdAndCategoryAndSourceLanguageAndTargetLanguage(
            Long userId, TerminologyEntry.TerminologyCategory category, String sourceLanguage, String targetLanguage);

    /**
     * 根据关键词和用户ID搜索术语条目
     * 
     * @param keyword 关键词
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 术语条目分页结果
     */
    @Query("SELECT te FROM TerminologyEntry te WHERE te.userId = :userId AND " +
           "(te.sourceTerm LIKE %:keyword% OR te.targetTerm LIKE %:keyword% OR te.notes LIKE %:keyword%) " +
           "ORDER BY te.createdAt DESC")
    Page<TerminologyEntry> searchByKeywordAndUserId(@Param("keyword") String keyword, @Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户ID统计术语条目数量
     * 
     * @param userId 用户ID
     * @return 术语条目数量
     */
    long countByUserId(Long userId);

    /**
     * 根据用户ID获取分类统计
     * 
     * @param userId 用户ID
     * @return 分类统计结果
     */
    @Query("SELECT te.category, COUNT(te) FROM TerminologyEntry te WHERE te.userId = :userId GROUP BY te.category ORDER BY COUNT(te) DESC")
    List<Object[]> getCategoryCountsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID获取语言对统计
     * 
     * @param userId 用户ID
     * @return 语言对统计结果
     */
    @Query("SELECT te.sourceLanguage, te.targetLanguage, COUNT(te) FROM TerminologyEntry te WHERE te.userId = :userId GROUP BY te.sourceLanguage, te.targetLanguage ORDER BY COUNT(te) DESC")
    List<Object[]> getLanguagePairCountsByUserId(@Param("userId") Long userId);

    /**
     * 获取所有分类统计
     * 
     * @return 分类统计结果
     */
    @Query("SELECT te.category, COUNT(te) FROM TerminologyEntry te GROUP BY te.category ORDER BY COUNT(te) DESC")
    List<Object[]> getCategoryCounts();

    /**
     * 获取所有语言对统计
     * 
     * @return 语言对统计结果
     */
    @Query("SELECT te.sourceLanguage, te.targetLanguage, COUNT(te) FROM TerminologyEntry te GROUP BY te.sourceLanguage, te.targetLanguage ORDER BY COUNT(te) DESC")
    List<Object[]> getLanguagePairCounts();

    /**
     * 根据用户ID获取不同分类列表
     * 
     * @param userId 用户ID
     * @return 分类列表
     */
    @Query("SELECT DISTINCT te.category FROM TerminologyEntry te WHERE te.userId = :userId AND te.category IS NOT NULL ORDER BY te.category")
    List<TerminologyEntry.TerminologyCategory> findDistinctCategoriesByUserId(@Param("userId") Long userId);

    /**
     * 获取所有不同分类列表
     * 
     * @return 分类列表
     */
    @Query("SELECT DISTINCT te.category FROM TerminologyEntry te WHERE te.category IS NOT NULL ORDER BY te.category")
    List<TerminologyEntry.TerminologyCategory> findDistinctCategories();

    /**
     * 根据源术语、目标术语、语言对和用户ID查找术语条目
     * 
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param userId 用户ID
     * @return 术语条目
     */
    Optional<TerminologyEntry> findBySourceTermAndTargetTermAndSourceLanguageAndTargetLanguageAndUserId(
            String sourceTerm, String targetTerm, String sourceLanguage, String targetLanguage, Long userId);

    /**
     * 根据关键字和过滤器搜索术语条目
     *
     * @param keyword        关键字
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param category       分类
     * @param domain         领域
     * @param userId         创建者ID
     * @param pageable       分页参数
     * @return 术语条目分页结果
     */
    @Query("SELECT te FROM TerminologyEntry te WHERE " +
            "(:keyword IS NULL OR te.sourceTerm LIKE %:keyword% OR te.targetTerm LIKE %:keyword%) AND " +
            "(:sourceLanguage IS NULL OR te.sourceLanguage = :sourceLanguage) AND " +
            "(:targetLanguage IS NULL OR te.targetLanguage = :targetLanguage) AND " +
            "(:category IS NULL OR te.category = :category) AND " +
            "(:domain IS NULL OR te.domain = :domain) AND " +
            "(:userId IS NULL OR te.userId = :userId)")
    Page<TerminologyEntry> findByKeywordAndFilters(
            @Param("keyword") String keyword,
            @Param("sourceLanguage") String sourceLanguage,
            @Param("targetLanguage") String targetLanguage,
            @Param("category") TerminologyEntry.TerminologyCategory category,
            @Param("domain") String domain,
            @Param("userId") Long userId,
            Pageable pageable);
}