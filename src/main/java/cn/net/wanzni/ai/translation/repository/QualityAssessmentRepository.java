package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.QualityAssessment;
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
 * 翻译质量评估数据访问接口
 * 
 * 提供质量评估的CRUD操作和自定义查询方法
 * 
 * @version 1.0.0
 */
@Repository
public interface QualityAssessmentRepository extends JpaRepository<QualityAssessment, Long> {

    /**
     * 根据翻译记录ID查找质量评估
     * 
     * @param translationRecordId 翻译记录ID
     * @return 质量评估
     */
    Optional<QualityAssessment> findByTranslationRecordId(Long translationRecordId);

    /**
     * 根据评估模式查找质量评估
     * 
     * @param assessmentMode 评估模式
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByAssessmentModeOrderByCreatedAtDesc(
            QualityAssessment.AssessmentMode assessmentMode, Pageable pageable);

    /**
     * 根据整体评分范围查找质量评估
     * 
     * @param minScore 最低评分
     * @param maxScore 最高评分
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByOverallScoreBetweenOrderByOverallScoreDesc(
            Integer minScore, Integer maxScore, Pageable pageable);

    /**
     * 查找高质量评估（整体评分大于等于指定分数）
     * 
     * @param minScore 最低评分
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByOverallScoreGreaterThanEqualOrderByOverallScoreDesc(
            Integer minScore, Pageable pageable);

    /**
     * 根据评估者ID查找质量评估
     * 
     * @param assessorId 评估者ID
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByAssessorIdOrderByCreatedAtDesc(String assessorId, Pageable pageable);

    /**
     * 根据时间范围查找质量评估
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查找人工评估的记录
     * 
     * @param pageable 分页参数
     * @return 人工评估分页结果
     */
    Page<QualityAssessment> findByIsManualAssessmentTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找自动评估的记录
     * 
     * @param pageable 分页参数
     * @return 自动评估分页结果
     */
    Page<QualityAssessment> findByIsManualAssessmentFalseOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 获取整体平均评分
     * 
     * @return 平均整体评分
     */
    @Query("SELECT AVG(qa.overallScore) FROM QualityAssessment qa")
    Optional<Double> getAverageOverallScore();

    /**
     * 获取各维度的平均评分
     * 
     * @return 各维度平均评分
     */
    @Query("SELECT AVG(qa.accuracyScore), AVG(qa.fluencyScore), AVG(qa.consistencyScore), AVG(qa.completenessScore) FROM QualityAssessment qa")
    List<Object[]> getAverageDimensionScores();

    /**
     * 根据评估引擎统计评估数量
     * 
     * @return 评估引擎统计结果
     */
    @Query("SELECT qa.assessmentEngine, COUNT(qa) FROM QualityAssessment qa WHERE qa.assessmentEngine IS NOT NULL GROUP BY qa.assessmentEngine ORDER BY COUNT(qa) DESC")
    List<Object[]> countByAssessmentEngine();

    /**
     * 统计各评估模式的数量
     * 
     * @return 评估模式统计结果
     */
    @Query("SELECT qa.assessmentMode, COUNT(qa) FROM QualityAssessment qa GROUP BY qa.assessmentMode")
    List<Object[]> countByAssessmentMode();

    /**
     * 获取评分分布统计
     * 
     * @return 评分分布统计结果
     */
    @Query("SELECT " +
           "SUM(CASE WHEN qa.overallScore >= 90 THEN 1 ELSE 0 END) as excellent, " +
           "SUM(CASE WHEN qa.overallScore >= 80 AND qa.overallScore < 90 THEN 1 ELSE 0 END) as good, " +
           "SUM(CASE WHEN qa.overallScore >= 70 AND qa.overallScore < 80 THEN 1 ELSE 0 END) as average, " +
           "SUM(CASE WHEN qa.overallScore >= 60 AND qa.overallScore < 70 THEN 1 ELSE 0 END) as fair, " +
           "SUM(CASE WHEN qa.overallScore < 60 THEN 1 ELSE 0 END) as poor " +
           "FROM QualityAssessment qa")
    List<Object[]> getScoreDistribution();

    /**
     * 查找最高评分的评估记录
     * 
     * @param pageable 分页参数
     * @return 最高评分的评估记录
     */
    Page<QualityAssessment> findTopByOrderByOverallScoreDesc(Pageable pageable);

    /**
     * 查找最低评分的评估记录
     * 
     * @param pageable 分页参数
     * @return 最低评分的评估记录
     */
    Page<QualityAssessment> findTopByOrderByOverallScoreAsc(Pageable pageable);

    /**
     * 获取平均评估时间
     * 
     * @return 平均评估时间（毫秒）
     */
    @Query("SELECT AVG(qa.assessmentTime) FROM QualityAssessment qa WHERE qa.assessmentTime IS NOT NULL")
    Optional<Double> getAverageAssessmentTime();

    /**
     * 根据准确性评分范围查找评估
     * 
     * @param minScore 最低准确性评分
     * @param maxScore 最高准确性评分
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByAccuracyScoreBetweenOrderByAccuracyScoreDesc(
            Integer minScore, Integer maxScore, Pageable pageable);

    /**
     * 根据流畅性评分范围查找评估
     * 
     * @param minScore 最低流畅性评分
     * @param maxScore 最高流畅性评分
     * @param pageable 分页参数
     * @return 质量评估分页结果
     */
    Page<QualityAssessment> findByFluencyScoreBetweenOrderByFluencyScoreDesc(
            Integer minScore, Integer maxScore, Pageable pageable);

    /**
     * 查找需要改进的翻译（整体评分低于指定分数）
     * 
     * @param maxScore 最高评分阈值
     * @param pageable 分页参数
     * @return 需要改进的评估分页结果
     */
    Page<QualityAssessment> findByOverallScoreLessThanOrderByOverallScoreAsc(
            Integer maxScore, Pageable pageable);

    /**
     * 统计指定时间范围内的评估数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 评估数量
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找特定评估者的平均评分
     * 
     * @param assessorId 评估者ID
     * @return 平均评分
     */
    @Query("SELECT AVG(qa.overallScore) FROM QualityAssessment qa WHERE qa.assessorId = :assessorId")
    Optional<Double> getAverageScoreByAssessor(@Param("assessorId") String assessorId);

    /**
     * 查找评估时间最长的记录
     * 
     * @param pageable 分页参数
     * @return 评估时间最长的记录
     */
    Page<QualityAssessment> findByAssessmentTimeIsNotNullOrderByAssessmentTimeDesc(Pageable pageable);

    /**
     * 删除指定时间之前的评估记录
     * 
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    long deleteByCreatedAtBefore(LocalDateTime beforeTime);

    /**
     * 查找各维度评分差异较大的评估（可能需要人工复核）
     * 
     * @param maxDifference 最大评分差异
     * @param pageable 分页参数
     * @return 评分差异较大的评估
     */
    @Query("SELECT qa FROM QualityAssessment qa WHERE " +
           "(ABS(qa.accuracyScore - qa.fluencyScore) > :maxDiff OR " +
           "ABS(qa.accuracyScore - qa.consistencyScore) > :maxDiff OR " +
           "ABS(qa.accuracyScore - qa.completenessScore) > :maxDiff OR " +
           "ABS(qa.fluencyScore - qa.consistencyScore) > :maxDiff OR " +
           "ABS(qa.fluencyScore - qa.completenessScore) > :maxDiff OR " +
           "ABS(qa.consistencyScore - qa.completenessScore) > :maxDiff) " +
           "ORDER BY qa.createdAt DESC")
    Page<QualityAssessment> findAssessmentsWithLargeDimensionDifferences(
            @Param("maxDiff") Integer maxDifference, Pageable pageable);
}