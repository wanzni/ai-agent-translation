package cn.net.susan.ai.translation.service.impl;

import cn.net.susan.ai.translation.config.PointsProperties;
import cn.net.susan.ai.translation.dto.LanguageDetectionResponse;
import cn.net.susan.ai.translation.dto.RagContext;
import cn.net.susan.ai.translation.dto.SupportedLanguageResponse;
import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.dto.TranslationResponse;
import cn.net.susan.ai.translation.dto.TranslationStatisticsResponse;
import cn.net.susan.ai.translation.entity.TranslationRecord;
import cn.net.susan.ai.translation.exception.InsufficientPointsException;
import cn.net.susan.ai.translation.repository.TranslationRecordRepository;
import cn.net.susan.ai.translation.service.MembershipService;
import cn.net.susan.ai.translation.service.PointsService;
import cn.net.susan.ai.translation.service.TranslationService;
import cn.net.susan.ai.translation.service.llm.RagService;
import cn.net.susan.ai.translation.service.translation.AliyunTranslationService;
import cn.net.susan.ai.translation.service.translation.QwenTranslationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 翻译服务实现类
 * 
 * 提供翻译功能的具体实现，集成阿里云翻译服务
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    private final TranslationRecordRepository translationRecordRepository;
    private final AliyunTranslationService aliyunTranslationService;
    private final QwenTranslationService qwenTranslationService;
    private final RagService ragService;
    private final PointsService pointsService;
    private final MembershipService membershipService;
    private final PointsProperties pointsProperties;

    // Mock支持的语言列表
    private static final Map<String, String> SUPPORTED_LANGUAGES = Map.of(
        "zh", "中文",
        "en", "English",
        "ja", "日本語",
        "ko", "한국어",
        "fr", "Français",
        "de", "Deutsch",
        "es", "Español",
        "ru", "Русский",
        "ar", "العربية",
        "pt", "Português"
    );

    // Mock翻译引擎列表
    private static final List<String> AVAILABLE_ENGINES = Arrays.asList(
        "ALIBABA_CLOUD", "QWEN", "TENCENT", "GOOGLE", "MICROSOFT", "DEEPL"
    );

    // Mock翻译结果映射（用于演示）
    private static final Map<String, Map<String, String>> MOCK_TRANSLATIONS = Map.of(
        "zh-en", Map.of(
            "你好", "Hello",
            "谢谢", "Thank you",
            "再见", "Goodbye",
            "智能翻译助手", "Translation AI Agent",
            "人工智能", "Artificial Intelligence"
        ),
        "en-zh", Map.of(
            "Hello", "你好",
            "Thank you", "谢谢",
            "Goodbye", "再见",
            "Artificial Intelligence", "人工智能",
            "Machine Learning", "机器学习"
        ),
        "zh-ja", Map.of(
            "你好", "こんにちは",
            "谢谢", "ありがとう",
            "再见", "さようなら"
        )
    );

    /**
     * 执行文本翻译。
     *
     * @param request 翻译请求
     * @return 翻译结果
     * @throws Exception 翻译过程中发生的异常
     */
    @Override
    public TranslationResponse translate(TranslationRequest request) throws Exception {
        log.info("开始翻译: {} -> {}, 文本: {}", 
                request.getSourceLanguage(), 
                request.getTargetLanguage(), 
                request.getSourceText().substring(0, Math.min(50, request.getSourceText().length())));

        // 验证请求参数
        if (!request.isValid()) {
            throw new IllegalArgumentException("翻译请求参数无效");
        }

        // 检查语言对支持
        if (!isLanguagePairSupported(request.getSourceLanguage(), request.getTargetLanguage())) {
            throw new UnsupportedOperationException("不支持的语言对: " + request.getLanguagePair());
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // 检查缓存
            String cachedTranslation = getTranslationCache(
                request.getSourceText(), 
                request.getSourceLanguage(), 
                request.getTargetLanguage()
            );
            
            TranslationResponse response;

                if (cachedTranslation != null) {
                    // 使用缓存结果
                    Long currentBalance = null;
                    if (request.getUserId() != null) {
                        try { currentBalance = pointsService.getBalance(request.getUserId()); } catch (Exception ignore) {}
                    }
                    response = TranslationResponse.builder()
                            .translatedText(cachedTranslation)
                            .sourceText(request.getSourceText())
                            .sourceLanguage(request.getSourceLanguage())
                            .targetLanguage(request.getTargetLanguage())
                            .translationEngine(request.getTranslationEngine())
                            .processingTime(System.currentTimeMillis() - startTime)
                            .status("COMPLETED")
                            .characterCount(request.getSourceText().length())
                            .qualityScore(95.0)
                            .usedPoints(0L)
                            .pointsBalance(currentBalance)
                            .build();
                        
                log.info("使用缓存翻译结果");
            } else {
                // 未命中缓存：先进行扣点评估与扣减（命中缓存不扣点）
                Long userId = request.getUserId();
                long requiredPoints = estimateRequiredPoints(request.getSourceText());
                Long usedPoints = null;
                if (userId != null && requiredPoints > 0) {
                    long balance = 0L;
                    try { balance = Math.max(0, pointsService.getBalance(userId)); } catch (Exception ignore) {}
                    long pointsConsume = Math.min(requiredPoints, balance);
                    long rest = requiredPoints - pointsConsume;
                    long membershipRemain = 0L;
                    try { membershipRemain = Math.max(0, membershipService.getRemainingQuota(userId)); } catch (Exception ignore) {}
                    if (membershipRemain < rest) {
                        throw new InsufficientPointsException("点数余额与会员配额不足，无法完成本次翻译");
                    }
                    usedPoints = pointsConsume;
                    performDeduction(userId, requiredPoints, null);
                }
                // 构建RAG上下文（当启用RAG或使用术语库时）
                if (Boolean.TRUE.equals(request.getUseRag()) || Boolean.TRUE.equals(request.getUseTerminology())) {
                    try {
                        // 使用结构化实体构建上下文，再转换为 Map 以兼容现有 DTO 字段
                        RagContext rag = ragService.buildRagContext(request);
                        Map<String, Object> ragCtx = new java.util.LinkedHashMap<>();
                        ragCtx.put("glossaryMap", rag.getGlossaryMap());
                        ragCtx.put("historySnippets", rag.getHistorySnippets());
                        ragCtx.put("contextSnippets", rag.getContextSnippets());
                        ragCtx.put("keywords", rag.getKeywords());
                        ragCtx.put("topK", rag.getTopK());
                        ragCtx.put("buildTimeMs", rag.getBuildTimeMs());
                        ragCtx.put("preprocessedSourceText", rag.getPreprocessedSourceText());
                        request.setRagContext(ragCtx);
                    } catch (Exception e) {
                        log.warn("构建RAG上下文失败，继续进行翻译: {}", e.getMessage());
                    }
                }
                // 根据翻译引擎选择实际翻译服务
                if ("ALIBABA_CLOUD".equals(request.getTranslationEngine())) {
                    response = aliyunTranslationService.translate(request);
                } else if ("QWEN".equalsIgnoreCase(request.getTranslationEngine())) {
                    response = qwenTranslationService.translate(request);
                } else {
                    // 使用Mock翻译作为后备
                    response = performMockTranslation(request, startTime);
                }
                
                // 如果翻译成功，保存到缓存
                if (response.isSuccessful()) {
                    setTranslationCache(
                        request.getSourceText(),
                        request.getSourceLanguage(),
                        request.getTargetLanguage(),
                        response.getTranslatedText()
                    );
                }

                // 注入点数字段
                if (userId != null) {
                    Long balance = null;
                    try { balance = pointsService.getBalance(userId); } catch (Exception ignore) {}
                    response.setUsedPoints(usedPoints != null ? usedPoints : 0L);
                    response.setPointsBalance(balance);
                }
            }
            
            // 保存翻译记录
            if (response.isSuccessful()) {
                TranslationRecord record = createTranslationRecord(request, response.getTranslatedText(),
                        response.getProcessingTime() != null ? response.getProcessingTime() : (System.currentTimeMillis() - startTime));
                translationRecordRepository.save(record);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("翻译失败: {}", e.getMessage(), e);
            
            // 返回错误响应
            Long errorBalance = null;
            if (request.getUserId() != null) {
                try { errorBalance = pointsService.getBalance(request.getUserId()); } catch (Exception ignore) {}
            }
            return TranslationResponse.builder()
                    .sourceText(request.getSourceText())
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .translationEngine(request.getTranslationEngine())
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .characterCount(request.getSourceText().length())
                    .processingTime(System.currentTimeMillis() - startTime)
                    .usedPoints(0L)
                    .pointsBalance(errorBalance)
                    .build();
        }
    }

    /**
     * 检测给定文本的语言。
     *
     * @param text 待检测文本
     * @return 检测结果，包含语言代码和置信度
     * @throws Exception 检测过程中发生的异常
     */
    @Override
    @Cacheable(value = "languageDetection", key = "#text.hashCode()")
    public LanguageDetectionResponse detectLanguage(String text) throws Exception {
        log.info("开始语言检测，文本长度: {}", text.length());
        long startTime = System.currentTimeMillis();
        
        try {
            // 优先使用 Qwen（DashScope）语言检测
            return qwenTranslationService.detectLanguage(text);
        } catch (Exception e) {
            log.warn("Qwen 语言检测失败，尝试阿里云检测: {}", e.getMessage());

            try {
                // 二级降级：尝试阿里云语言检测
                return aliyunTranslationService.detectLanguage(text);
            } catch (Exception e2) {
                log.warn("阿里云语言检测也失败，使用Mock检测: {}", e2.getMessage());

                // 使用Mock检测作为最终后备
                String detectedLanguage = performMockLanguageDetection(text);
                return LanguageDetectionResponse.builder()
                        .language(detectedLanguage)
                        .confidence(generateMockConfidence())
                        .success(true)
                        .processingTime(System.currentTimeMillis() - startTime)
                        .build();
            }
        }
    }

    /**
     * 获取所有支持的语言列表。
     *
     * @return 语言代码和名称的映射
     * @throws Exception 获取过程中发生的异常
     */
    @Override
    @Cacheable(value = "supportedLanguages")
    public List<SupportedLanguageResponse> getSupportedLanguages() throws Exception {
        log.info("获取支持的语言列表");
        
        return SUPPORTED_LANGUAGES.entrySet().stream()
                .map(entry -> SupportedLanguageResponse.builder()
                        .languageCode(entry.getKey())
                        .languageName(entry.getValue())
                        .nativeName(entry.getValue())
                        .isActive(true)
                        .sortOrder(0)
                        .build())
                .toList();
    }

    /**
     * 分页获取翻译历史记录。
     *
     * @param userId 用户ID（可选）
     * @param pageable 分页参数
     * @return 翻译历史分页结果
     * @throws Exception 查询过程中发生的异常
     */
    @Override
    public Page<TranslationRecord> getTranslationHistory(Long userId, Pageable pageable) throws Exception {
        log.info("获取翻译历史: userId={}, page={}, size={}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());

        if (userId != null) {
            return translationRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            return translationRecordRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    /**
     * 根据 ID 获取指定的翻译记录。
     *
     * @param id 翻译记录ID
     * @return 翻译记录，如果不存在返回null
     * @throws Exception 查询过程中发生的异常
     */
    @Override
    public TranslationRecord getTranslationById(Long id) throws Exception {
        log.info("获取翻译记录: id={}", id);
        return translationRecordRepository.findById(id).orElse(null);
    }

    /**
     * 根据 ID 删除指定的翻译记录。
     *
     * @param id 翻译记录ID
     * @return 是否删除成功
     * @throws Exception 删除过程中发生的异常
     */
    @Override
    public boolean deleteTranslation(Long id) throws Exception {
        log.info("删除翻译记录: id={}", id);
        
        if (translationRecordRepository.existsById(id)) {
            translationRecordRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 获取指定用户的翻译统计信息。
     *
     * @param userId 用户ID（可选）
     * @return 统计信息
     * @throws Exception 统计过程中发生的异常
     */
    @Override
    public TranslationStatisticsResponse getTranslationStatistics(Long userId) throws Exception {
        log.info("获取翻译统计信息: userId={}", userId);

        Long totalTranslations;
        Long totalCharacters;
        Double averageQualityScore;

        if (userId != null) {
            // 用户统计
            totalTranslations = translationRecordRepository.countByUserId(userId);
            totalCharacters = translationRecordRepository.sumCharactersByUserId(userId).orElse(0L);
            averageQualityScore = translationRecordRepository.getAverageQualityScoreByUserId(userId).orElse(0.0);
        } else {
            // 全局统计
            totalTranslations = translationRecordRepository.count();
            totalCharacters = translationRecordRepository.sumAllCharacters().orElse(0L);
            averageQualityScore = translationRecordRepository.getAverageQualityScore().orElse(0.0);
        }

        return TranslationStatisticsResponse.builder()
                .totalTranslations(totalTranslations)
                .totalCharacters(totalCharacters)
                .averageQualityScore(averageQualityScore)
                .todayTranslations(0L) // Mock数据
                .weekTranslations(0L) // Mock数据
                .monthTranslations(0L) // Mock数据
                .build();
    }

    /**
     * 对指定的翻译记录进行重新翻译。
     *
     * @param id 原翻译记录ID
     * @param engine 新的翻译引擎（可选）
     * @return 新的翻译结果
     * @throws Exception 翻译过程中发生的异常
     */
    @Override
    public TranslationResponse retranslate(Long id, String engine) throws Exception {
        log.info("重新翻译: id={}, engine={}", id, engine);

        TranslationRecord original = getTranslationById(id);
        if (original == null) {
            throw new IllegalArgumentException("翻译记录不存在: " + id);
        }

        // 创建新的翻译请求
        TranslationRequest request = TranslationRequest.builder()
                .sourceText(original.getSourceText())
                .sourceLanguage(original.getSourceLanguage())
                .targetLanguage(original.getTargetLanguage())
                .translationType(original.getTranslationType().name())
                .translationEngine(engine != null ? engine : original.getTranslationEngine())
                .useTerminology(original.getUseTerminology())
                .userId(original.getUserId())
                .build();

        return translate(request);
    }

    /**
     * 根据文本内容搜索翻译记录。
     *
     * @param keyword 搜索关键词
     * @param userId 用户ID（可选）
     * @param pageable 分页参数
     * @return 搜索结果
     * @throws Exception 搜索过程中发生的异常
     */
    @Override
    public Page<TranslationRecord> searchTranslations(String keyword, Long userId, Pageable pageable) throws Exception {
        log.info("搜索翻译记录: keyword={}, userId={}", keyword, userId);

        if (userId != null) {
            return translationRecordRepository.searchByTextContentAndUserId(keyword, userId, pageable);
        } else {
            return translationRecordRepository.searchByTextContent(keyword, pageable);
        }
    }

    /**
     * 获取指定翻译记录的质量评估结果。
     *
     * @param translationId 翻译记录ID
     * @return 质量评估结果
     * @throws Exception 评估过程中发生的异常
     */
    @Override
    public Map<String, Object> getQualityAssessment(Long translationId) throws Exception {
        log.info("获取翻译质量评估: translationId={}", translationId);

        TranslationRecord record = getTranslationById(translationId);
        if (record == null) {
            throw new IllegalArgumentException("翻译记录不存在: " + translationId);
        }

        // Mock质量评估结果
        Map<String, Object> assessment = new HashMap<>();
        assessment.put("overallScore", record.getQualityScore());
        assessment.put("accuracyScore", generateMockScore(80, 100));
        assessment.put("fluencyScore", generateMockScore(75, 95));
        assessment.put("consistencyScore", generateMockScore(70, 90));
        assessment.put("completenessScore", generateMockScore(85, 100));
        assessment.put("suggestions", generateMockSuggestions());
        assessment.put("strengths", generateMockStrengths());
        assessment.put("improvements", generateMockImprovements());

        return assessment;
    }

    /**
     * 获取所有可用的翻译引擎列表。
     *
     * @return 可用的翻译引擎列表
     */
    @Override
    public List<String> getAvailableEngines() {
        return new ArrayList<>(AVAILABLE_ENGINES);
    }

    /**
     * 验证指定的语言对是否受支持。
     *
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 是否支持该语言对
     */
    @Override
    public boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage) {
        // 允许源语言为自动检测
        if (sourceLanguage == null || "auto".equalsIgnoreCase(sourceLanguage)) {
            return SUPPORTED_LANGUAGES.containsKey(targetLanguage);
        }
        return SUPPORTED_LANGUAGES.containsKey(sourceLanguage)
                && SUPPORTED_LANGUAGES.containsKey(targetLanguage)
                && !sourceLanguage.equalsIgnoreCase(targetLanguage);
    }

    /**
     * 从缓存中获取翻译结果。
     *
     * @param sourceText 源文本
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 缓存的翻译结果，如果没有缓存则返回null
     */
    @Override
    @Cacheable(value = "translationCache", key = "#sourceText.hashCode() + '_' + #sourceLanguage + '_' + #targetLanguage")
    public String getTranslationCache(String sourceText, String sourceLanguage, String targetLanguage) {
        // 缓存由Spring Cache管理，这里返回null表示没有缓存
        return null;
    }

    /**
     * 将翻译结果设置到缓存中。
     *
     * @param sourceText 源文本
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param translatedText 翻译结果
     */
    @Override
    public void setTranslationCache(String sourceText, String sourceLanguage, String targetLanguage, String translatedText) {
        // 缓存设置由Spring Cache的@Cacheable注解自动处理
        log.debug("设置翻译缓存: {} -> {}", sourceText.substring(0, Math.min(20, sourceText.length())), 
                translatedText.substring(0, Math.min(20, translatedText.length())));
    }

    // ========== 私有辅助方法 ==========

    /**
     * 执行Mock翻译
     *
     * @param request 翻译请求
     * @return 翻译结果
     */
    private String performMockTranslation(TranslationRequest request) {
        String languagePair = request.getLanguagePair();
        String sourceText = request.getSourceText();

        // 检查是否有预定义的翻译
        Map<String, String> translations = MOCK_TRANSLATIONS.get(languagePair);
        if (translations != null && translations.containsKey(sourceText)) {
            return translations.get(sourceText);
        }

        // 生成Mock翻译结果
        return generateMockTranslation(sourceText, request.getTargetLanguage());
    }

    /**
     * 估算本次文本翻译所需点数。
     * 统一规则：文本翻译固定消耗 1 点数。
     *
     * @param text 文本内容
     * @return 所需点数
     */
    private long estimateRequiredPoints(String text) {
        return pointsProperties.getTextDeduction();
    }

    /**
     * 执行扣点：优先扣减点数余额，不足部分再消耗会员配额；都不足则抛异常。
     *
     * @param userId 用户ID
     * @param requiredPoints 所需点数
     * @param referenceId 引用ID
     */
    private void performDeduction(Long userId, long requiredPoints, String referenceId) {
        long balance = pointsService.getBalance(userId);
        if (balance >= requiredPoints) {
            pointsService.deduct(userId, requiredPoints, "文本翻译扣点", referenceId);
            return;
        }
        if (balance > 0) {
            pointsService.deduct(userId, balance, "文本翻译扣点", referenceId);
            long rest = requiredPoints - balance;
            long membershipRemain = membershipService.getRemainingQuota(userId);
            if (membershipRemain >= rest) {
                membershipService.consumeQuota(userId, rest);
                return;
            }
            throw new InsufficientPointsException("点数余额与会员配额不足，无法完成本次翻译");
        }
        long membershipRemain = membershipService.getRemainingQuota(userId);
        if (membershipRemain >= requiredPoints) {
            membershipService.consumeQuota(userId, requiredPoints);
            return;
        }
        throw new InsufficientPointsException("点数余额与会员配额不足，无法完成本次翻译");
    }

    /**
     * 生成Mock翻译结果
     *
     * @param sourceText 源文本
     * @param targetLanguage 目标语言
     * @return Mock翻译结果
     */
    private String generateMockTranslation(String sourceText, String targetLanguage) {
        // 简单的Mock翻译逻辑
        switch (targetLanguage) {
            case "en":
                return "[EN] " + sourceText;
            case "zh":
                return "[中文] " + sourceText;
            case "ja":
                return "[日本語] " + sourceText;
            case "ko":
                return "[한국어] " + sourceText;
            case "fr":
                return "[FR] " + sourceText;
            case "de":
                return "[DE] " + sourceText;
            case "es":
                return "[ES] " + sourceText;
            case "ru":
                return "[RU] " + sourceText;
            case "ar":
                return "[AR] " + sourceText;
            case "pt":
                return "[PT] " + sourceText;
            default:
                return "[TRANSLATED] " + sourceText;
        }
    }

    /**
     * 执行Mock语言检测
     *
     * @param text 待检测文本
     * @return 检测到的语言代码
     */
    private String performMockLanguageDetection(String text) {
        // 简单的语言检测逻辑
        if (text.matches(".*[\\u4e00-\\u9fff].*")) {
            return "zh";
        } else if (text.matches(".*[\\u3040-\\u309f\\u30a0-\\u30ff].*")) {
            return "ja";
        } else if (text.matches(".*[\\uac00-\\ud7af].*")) {
            return "ko";
        } else if (text.matches(".*[\\u0600-\\u06ff].*")) {
            return "ar";
        } else if (text.matches(".*[\\u0400-\\u04ff].*")) {
            return "ru";
        } else {
            return "en"; // 默认英文
        }
    }

    /**
     * 创建翻译记录
     *
     * @param request 翻译请求
     * @param translatedText 翻译结果
     * @param processingTime 处理时间
     * @return 翻译记录
     */
    private TranslationRecord createTranslationRecord(TranslationRequest request, String translatedText, long processingTime) {
        return TranslationRecord.builder()
                .userId(request.getUserId())
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .sourceText(request.getSourceText())
                .translatedText(translatedText)
                .translationType(resolveTranslationType(request.getTranslationType()))
                .translationEngine(request.getTranslationEngine())
                .qualityScore(generateMockQualityScore().intValue())
                .processingTime(processingTime)
                .characterCount(request.getCharacterCount())
                .useTerminology(request.getUseTerminology())
                .status(TranslationRecord.TranslationStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 解析前端传入的翻译类型字符串为枚举，兼容多种别名并提供安全默认值。
     *
     * @param type 翻译类型字符串
     * @return 翻译类型枚举
     */
    private TranslationRecord.TranslationType resolveTranslationType(String type) {
        if (type == null || type.isBlank()) {
            return TranslationRecord.TranslationType.TEXT;
        }
        String t = type.trim().toUpperCase();
        // 兼容别名与历史值
        switch (t) {
            case "TEXT":
            case "STANDARD":
            case "SINGLE":
            case "MT":
                return TranslationRecord.TranslationType.TEXT;
            case "DOCUMENT":
            case "DOC":
            case "FILE":
            case "BATCH":
                return TranslationRecord.TranslationType.DOCUMENT;
            case "CHAT":
            case "CONVERSATION":
            case "DIALOG":
                return TranslationRecord.TranslationType.CHAT;
            default:
                // 未识别类型时默认按文本翻译处理，避免 IllegalArgumentException
                return TranslationRecord.TranslationType.TEXT;
        }
    }

    /**
     * 生成Mock质量评分
     *
     * @return Mock质量评分
     */
    private Double generateMockQualityScore() {
        return 75.0 + ThreadLocalRandom.current().nextDouble(25.0);
    }

    /**
     * 生成Mock置信度
     *
     * @return Mock置信度
     */
    private Double generateMockConfidence() {
        return 0.8 + ThreadLocalRandom.current().nextDouble(0.2);
    }

    /**
     * 生成Mock术语数量
     *
     * @return Mock术语数量
     */
    private Integer generateMockTerminologyCount() {
        return ThreadLocalRandom.current().nextInt(1, 6);
    }

    /**
     * 生成Mock评分
     *
     * @param min 最小值
     * @param max 最大值
     * @return Mock评分
     */
    private Double generateMockScore(int min, int max) {
        return (double) ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 生成替代语言
     *
     * @param detectedLanguage 检测到的语言
     * @return 替代语言列表
     */
    private List<Map<String, Object>> generateAlternativeLanguages(String detectedLanguage) {
        List<Map<String, Object>> alternatives = new ArrayList<>();
        
        SUPPORTED_LANGUAGES.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(detectedLanguage))
                .limit(3)
                .forEach(entry -> {
                    Map<String, Object> alt = new HashMap<>();
                    alt.put("language", entry.getKey());
                    alt.put("languageName", entry.getValue());
                    alt.put("confidence", 0.1 + ThreadLocalRandom.current().nextDouble(0.3));
                    alternatives.add(alt);
                });
        
        return alternatives;
    }

    /**
     * 生成Mock建议
     *
     * @return Mock建议列表
     */
    private List<String> generateMockSuggestions() {
        return Arrays.asList(
                "建议在专业术语翻译时使用术语库",
                "可以考虑调整语言风格以提高流畅性",
                "注意保持翻译的一致性"
        );
    }

    /**
     * 生成Mock优点
     *
     * @return Mock优点列表
     */
    private List<String> generateMockStrengths() {
        return Arrays.asList(
                "翻译准确度较高",
                "语法结构正确",
                "术语使用恰当"
        );
    }

    /**
     * 生成Mock改进点
     *
     * @return Mock改进点列表
     */
    private List<String> generateMockImprovements() {
        return Arrays.asList(
                "可以进一步优化语言表达的自然度",
                "建议增强上下文理解能力"
        );
    }

    /**
     * 生成Mock每日统计
     *
     * @return Mock每日统计
     */
    private Map<String, Object> generateMockDailyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", ThreadLocalRandom.current().nextInt(50, 200));
        stats.put("characters", ThreadLocalRandom.current().nextInt(5000, 20000));
        stats.put("averageTime", ThreadLocalRandom.current().nextInt(500, 2000));
        return stats;
    }

    /**
     * 生成Mock周趋势
     *
     * @return Mock周趋势列表
     */
    private List<Map<String, Object>> generateMockWeeklyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", LocalDateTime.now().minusDays(6 - i).toLocalDate().toString());
            day.put("count", ThreadLocalRandom.current().nextInt(20, 100));
            trend.add(day);
        }
        return trend;
    }

    /**
     * 生成Mock热门语言对
     *
     * @return Mock热门语言对列表
     */
    private List<Map<String, Object>> generateMockTopLanguagePairs() {
        List<Map<String, Object>> pairs = new ArrayList<>();
        
        String[][] topPairs = {
            {"zh", "en"}, {"en", "zh"}, {"zh", "ja"}, {"en", "fr"}, {"zh", "ko"}
        };
        
        for (String[] pair : topPairs) {
            Map<String, Object> pairStats = new HashMap<>();
            pairStats.put("sourceLanguage", pair[0]);
            pairStats.put("targetLanguage", pair[1]);
            pairStats.put("count", ThreadLocalRandom.current().nextInt(100, 1000));
            pairs.add(pairStats);
        }
        
        return pairs;
    }

    /**
     * 批量翻译多个文本。
     *
     * @param requests 包含多个翻译请求的列表
     * @return 包含每个请求翻译结果的列表
     * @throws Exception 如果批量翻译过程中发生严重错误
     */
    @Override
    public List<TranslationResponse> batchTranslate(List<TranslationRequest> requests) throws Exception {
        log.info("开始批量翻译，数量: {}", requests.size());

        List<TranslationResponse> responses = new ArrayList<>();
        
        for (TranslationRequest request : requests) {
            try {
                TranslationResponse response = translate(request);
                responses.add(response);
            } catch (Exception e) {
                log.error("批量翻译中单个请求失败: {}", e.getMessage());
                // 创建失败响应
                TranslationResponse errorResponse = TranslationResponse.builder()
                        .sourceText(request.getSourceText())
                        .sourceLanguage(request.getSourceLanguage())
                        .targetLanguage(request.getTargetLanguage())
                        .status("ERROR")
                        .errorMessage(e.getMessage())
                        .build();
                responses.add(errorResponse);
            }
        }

        return responses;
    }

    /**
     * 并行翻译专用线程池
     * - 核心线程数8，可同时翻译8个段落
     * - 最大16线程，应对突发大量翻译
     * - 有界队列1000，防止内存膨胀
     * - CallerRunsPolicy：队列满时让调用线程执行，起到背压作用
     */
    private static final Executor parallelTranslateExecutor = new ThreadPoolExecutor(
            8, 16, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "parallel-translate-" + count++);
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 并行批量翻译：利用多线程并行翻译多个段落，突破大模型Token限制
     * 
     * 工作原理：
     * 1. 将长文档分成多个小段落（如PDF每行一个段落）
     * 2. 为每个段落创建一个异步任务，使用CompletableFuture.supplyAsync()
     * 3. 所有任务并行执行（8线程）
     * 4. 使用CompletableFuture.allOf()等待所有任务完成
     * 5. 按原始顺序聚合结果
     * 
     * 性能提升：假设每段翻译2秒，10段并行只需 10÷8×2≈2.5秒（vs 串行20秒）
     *
     * @param requests 翻译请求列表（每个元素是一段文本）
     * @return 翻译结果列表（顺序与输入一致）
     * @throws Exception 翻译过程中发生的异常
     */

    public List<TranslationResponse> parallelBatchTranslate(List<TranslationRequest> requests) throws Exception {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        
        log.info("开始并行批量翻译，数量: {}", requests.size());
        long startTime = System.currentTimeMillis();
        
        // 第一步：为每个翻译请求创建一个CompletableFuture异步任务
        List<CompletableFuture<TranslationResponse>> futures = new ArrayList<>();
        
        for (TranslationRequest request : requests) {
            // supplyAsync: 异步执行有返回值的任务
            CompletableFuture<TranslationResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return translate(request);
                } catch (Exception e) {
                    log.error("并行翻译单个请求失败: {}", e.getMessage());
                    // 失败时返回错误响应，而不是抛异常，保证其他任务继续执行
                    return TranslationResponse.builder()
                            .sourceText(request.getSourceText())
                            .sourceLanguage(request.getSourceLanguage())
                            .targetLanguage(request.getTargetLanguage())
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build();
                }
            }, parallelTranslateExecutor);  // 使用专用线程池
            futures.add(future);
        }
        
        // 第二步：等待所有异步任务完成
        // allOf: 当所有CompletableFuture都完成时，主任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 第三步：按原始顺序收集结果
        List<TranslationResponse> responses = new ArrayList<>(futures.size());
        for (CompletableFuture<TranslationResponse> future : futures) {
            responses.add(future.join());  // 获取异步任务的结果
        }
        
        // 第四步：记录性能日志
        long duration = System.currentTimeMillis() - startTime;
        log.info("并行批量翻译完成，数量: {}, 耗时: {}ms, 提速比: {:.2f}x", 
                requests.size(), duration, 
                requests.size() > 0 ? (double) duration / Math.max(1, requests.size()) : 1);
        
        return responses;
    }

    /**
     * 执行Mock翻译（作为后备方案）
     *
     * @param request 翻译请求
     * @param startTime 开始时间
     * @return 翻译结果
     */
    private TranslationResponse performMockTranslation(TranslationRequest request, long startTime) {
        String translatedText = performMockTranslation(request);
        long duration = System.currentTimeMillis() - startTime;
        
        Long balance = null;
        if (request.getUserId() != null) {
            try { balance = pointsService.getBalance(request.getUserId()); } catch (Exception ignore) {}
        }
        return TranslationResponse.builder()
                .translatedText(translatedText)
                .sourceText(request.getSourceText())
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .translationEngine("MOCK")
                .processingTime(duration)
                .status("COMPLETED")
                .characterCount(request.getSourceText().length())
                .qualityScore(generateMockQualityScore())
                .createdAt(LocalDateTime.now())
                .usedPoints(0L)
                .pointsBalance(balance)
                .build();
    }
}