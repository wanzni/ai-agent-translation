package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.dto.*;
import cn.net.susan.ai.translation.service.TranslationService;
import cn.net.susan.ai.translation.service.PolishService;
import cn.net.susan.ai.translation.security.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 翻译API控制器
 * 
 * 提供翻译相关的RESTful API接口
 * 
 * @author 苏三
 * @version 1.0.0
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
@Validated
public class TranslationController {
    
    private final TranslationService translationService;
    private final PolishService polishService;
    
    /**
     * 执行文本翻译
     * 
     * @param request 翻译请求
     * @return 翻译结果
     */
    @PostMapping("/translate")
    public TranslationResponse translate(@Valid @RequestBody TranslationRequest request) throws Exception {
        log.info("收到翻译请求: {} -> {}", request.getSourceLanguage(), request.getTargetLanguage());
        
        // 统一从用户上下文填充用户ID（若上下文存在）
        if (request.getUserId() == null) {
            Long ctxUserId = UserContext.getUserId();
            if (ctxUserId != null) {
                request.setUserId(ctxUserId);
                log.info("已从用户上下文填充用户ID: {}", ctxUserId);
            } else {
                log.warn("用户上下文为空，未能填充用户ID");
            }
        }
        
        return translationService.translate(request);
    }
    
    /**
     * 批量翻译
     * 
     * @param requests 翻译请求列表
     * @return 翻译结果列表
     */
    @PostMapping("/batch-translate")
    public List<TranslationResponse> batchTranslate(@Valid @RequestBody List<TranslationRequest> requests) throws Exception {
        log.info("收到批量翻译请求，数量: {}", requests.size());
        
        // 统一从用户上下文填充用户ID（若上下文存在）
        Long ctxUserId = UserContext.getUserId();
        if (ctxUserId != null) {
            for (TranslationRequest req : requests) {
                if (req.getUserId() == null) {
                    req.setUserId(ctxUserId);
                }
            }
        } else {
            log.warn("批量翻译时用户上下文为空，未填充用户ID");
        }
        
        return translationService.batchTranslate(requests);
    }
    
    /**
     * 语言检测
     * 
     * @param text 待检测文本
     * @return 检测结果
     */
    @PostMapping("/detect-language")
    public LanguageDetectionResponse detectLanguage(
            @RequestBody @NotBlank(message = "检测文本不能为空") 
            @Size(max = 1000, message = "检测文本长度不能超过1000字符") String text) throws Exception {
        log.info("收到语言检测请求，文本长度: {}", text.length());
        return translationService.detectLanguage(text);
    }
    
    /**
     * 获取支持的语言列表
     * 
     * @return 支持的语言列表
     */
    @GetMapping("/supported-languages")
    public List<SupportedLanguageResponse> getSupportedLanguages() throws Exception {
        log.info("获取支持的语言列表");
        return translationService.getSupportedLanguages();
    }
    
    /**
     * 获取可用的翻译引擎列表
     * 
     * @return 翻译引擎列表
     */
    @GetMapping("/engines")
    public List<String> getAvailableEngines() throws Exception {
        log.info("获取可用翻译引擎列表");
        return translationService.getAvailableEngines();
    }
    
    /**
     * 检查语言对是否支持
     * 
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 是否支持
     */
    @GetMapping("/check-language-pair")
    public LanguagePairSupportResponse checkLanguagePair(
            @RequestParam @NotBlank(message = "源语言不能为空") String sourceLanguage,
            @RequestParam @NotBlank(message = "目标语言不能为空") String targetLanguage) throws Exception {
        log.info("检查语言对支持: {} -> {}", sourceLanguage, targetLanguage);
        boolean supported = translationService.isLanguagePairSupported(sourceLanguage, targetLanguage);
        return LanguagePairSupportResponse.builder()
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .supported(supported)
                .build();
    }

    /**
     * 依据术语库与风格 Prompt 对 MT 结果二次润色
     */
    @PostMapping("/polish")
    public PolishResponse polish(@Valid @RequestBody PolishRequest request) throws Exception {
        log.info("收到后编辑请求: targetLanguage={}, glossary filters: category={}, domain={}",
                request.getTargetLanguage(), request.getCategory(), request.getDomain());
        return polishService.polish(request);
    }
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.builder()
                .timestamp(System.currentTimeMillis())
                .service("Translation Service")
                .version("1.0.0")
                .build();
    }
}