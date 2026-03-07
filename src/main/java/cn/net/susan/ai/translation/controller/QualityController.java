package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.dto.QualityAssessmentRequest;
import cn.net.susan.ai.translation.dto.QualityAssessmentResponse;
import cn.net.susan.ai.translation.dto.TranslationRequest;
import cn.net.susan.ai.translation.service.QualityAssessmentService;
import cn.net.susan.ai.translation.service.translation.QwenTranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

/**
 * 翻译质量评估控制器，提供对翻译文本进行质量打分和分析的功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/quality")
@Validated
@RequiredArgsConstructor
public class QualityController {

    private final QualityAssessmentService qualityAssessmentService;
    private final QwenTranslationService translationService; // 用于必要时语言检测

    /**
     * 评估翻译质量。
     * <p>
     * 此端点接收源文本、目标文本和语言对，然后调用 {@link QualityAssessmentService} 进行质量评估。
     * 它还具备自动检测源语言和将评估结果（如改进建议）本地化为中文的功能。
     *
     * @param payload 包含待评估文本和语言信息的请求
     * @return {@link QualityAssessmentResponse} 包含评分、优势、改进建议等信息的评估结果
     * @throws Exception 如果评估过程中发生严重错误
     */
    @PostMapping("/assess")
    public QualityAssessmentResponse assess(@Valid @RequestBody QualityAssessmentRequest payload) throws Exception {
        // 自动检测源语言
        String sourceLang = payload.getSourceLanguage();
        if (!StringUtils.hasText(sourceLang) || "auto".equalsIgnoreCase(sourceLang)) {
            try {
                var detect = translationService.detectLanguage(payload.getSourceText());
                // LanguageDetectionResponse 使用字段名为 `language`
                sourceLang = detect.getLanguage();
                payload.setSourceLanguage(sourceLang);
            } catch (Exception e) {
                log.warn("语言检测失败，继续评估: {}", e.getMessage());
            }
        }

        QualityAssessmentResponse result = qualityAssessmentService.assess(payload);
        // 补充检测到的源语言返回
        if (StringUtils.hasText(sourceLang) && !StringUtils.hasText(result.getDetectedSourceLanguage())) {
            result.setDetectedSourceLanguage(sourceLang);
        }

        // 将改进建议始终本地化为中文；关注点与优势按目标语言可选本地化
        try {
            String targetLang = payload.getTargetLanguage();
            if (StringUtils.hasText(targetLang)) {
                // 翻译列表通用方法（目标：中文）
                Function<List<String>, List<String>> localizeToZh = (list) -> {
                    if (list == null || list.isEmpty()) return list;
                    java.util.List<String> out = new java.util.ArrayList<>();
                    for (String s : list) {
                        String text = (s == null) ? "" : s.trim();
                        if (text.isEmpty()) { out.add(text); continue; }
                        // 若已包含中文则跳过
                        if (text.chars().anyMatch(ch -> ch > 127)) { out.add(text); continue; }
                        try {
                            var resp = translationService.translate(
                                    TranslationRequest.builder()
                                            .sourceText(text)
                                            .sourceLanguage("auto")
                                            .targetLanguage("zh")
                                            .build()
                            );
                            out.add(resp.getTranslatedText() != null ? resp.getTranslatedText().trim() : text);
                        } catch (Exception ex) {
                            // 翻译失败则保留原文，避免中断整个评估
                            log.warn("建议本地化失败: {}", ex.getMessage());
                            out.add(text);
                        }
                    }
                    return out;
                };

                // 始终将改进建议转换为中文
                result.setImprovementSuggestions(localizeToZh.apply(result.getImprovementSuggestions()));

                // 当目标语言为中文时，同时本地化关注点与优势
                boolean needZh = targetLang.toLowerCase().startsWith("zh");
                if (needZh) {
                    result.setAttentionPoints(localizeToZh.apply(result.getAttentionPoints()));
                    result.setStrengths(localizeToZh.apply(result.getStrengths()));
                }
            }
        } catch (Exception le) {
            log.warn("本地化建议时发生异常: {}", le.getMessage());
        }

        return result;
    }
}