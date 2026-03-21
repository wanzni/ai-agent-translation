package cn.net.wanzni.ai.translation.service.translation;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.dto.LanguageDetectionResponse;
import com.aliyun.alimt20181012.Client;
import com.aliyun.alimt20181012.models.TranslateGeneralRequest;
import com.aliyun.alimt20181012.models.TranslateGeneralResponse;
import com.aliyun.alimt20181012.models.GetDetectLanguageRequest;
import com.aliyun.alimt20181012.models.GetDetectLanguageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 阿里云翻译服务实现类
 * 
 * 封装阿里云翻译API的调用逻辑
 * 
 * @version 1.0.0
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunTranslationService implements ThirdPartyTranslator {
    
    private final Client aliyunTranslationClient;
    
    /**
     * 执行阿里云文本翻译
     * 
     * @param request 翻译请求
     * @return 翻译响应
     * @throws Exception 翻译异常
     */
    @Override
    public TranslationResponse translate(TranslationRequest request) throws Exception {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        try {
            log.info("开始调用阿里云翻译API，请求ID: {}, 源语言: {}, 目标语言: {}", 
                    requestId, request.getSourceLanguage(), request.getTargetLanguage());
            
            // 构建阿里云翻译请求
            TranslateGeneralRequest translateRequest = new TranslateGeneralRequest()
                    .setSourceText(request.getSourceText())
                    .setSourceLanguage(convertLanguageCode(request.getSourceLanguage()))
                    .setTargetLanguage(convertLanguageCode(request.getTargetLanguage()))
                    .setScene("general")
                    .setFormatType("text");
            
            // 调用阿里云翻译API
            TranslateGeneralResponse response = aliyunTranslationClient.translateGeneral(translateRequest);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getBody() != null && response.getBody().getData() != null) {
                String translatedText = response.getBody().getData().getTranslated();
                
                log.info("阿里云翻译成功，请求ID: {}, 耗时: {}ms", requestId, duration);
                
                return TranslationResponse.builder()
                        .translatedText(translatedText)
                        .sourceText(request.getSourceText())
                        .sourceLanguage(request.getSourceLanguage())
                        .targetLanguage(request.getTargetLanguage())
                        .translationEngine("ALIBABA_CLOUD")
                        .processingTime(duration)
                        .status("COMPLETED")
                        .characterCount(request.getSourceText().length())
                        .qualityScore(95.0)
                        .createdAt(LocalDateTime.now())
                        .build();
            } else {
                throw new RuntimeException("阿里云翻译API返回结果为空");
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("阿里云翻译失败，请求ID: {}, 错误: {}", requestId, e.getMessage(), e);
            
            return TranslationResponse.builder()
                    .sourceText(request.getSourceText())
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .translationEngine("ALIBABA_CLOUD")
                    .processingTime(duration)
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .characterCount(request.getSourceText().length())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 执行语言检测
     * 
     * @param text 待检测文本
     * @return 语言检测结果
     * @throws Exception 检测异常
     */
    @Override
    public LanguageDetectionResponse detectLanguage(String text) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始调用阿里云语言检测API，文本长度: {}", text.length());
            
            GetDetectLanguageRequest detectRequest = new GetDetectLanguageRequest()
                    .setSourceText(text);
            
            GetDetectLanguageResponse response = aliyunTranslationClient.getDetectLanguage(detectRequest);
            
            if (response.getBody() != null && response.getBody().getDetectedLanguage() != null) {
                String detectedLanguage = response.getBody().getDetectedLanguage();
                
                log.info("阿里云语言检测成功，检测结果: {}", detectedLanguage);
                
                return LanguageDetectionResponse.builder()
                        .language(convertToStandardLanguageCode(detectedLanguage))
                        .confidence(0.95) // 阿里云检测准确率较高
                        .success(true)
                        .processingTime(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                throw new RuntimeException("阿里云语言检测API返回结果为空");
            }
            
        } catch (Exception e) {
            log.error("阿里云语言检测失败，错误: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 将标准语言代码转换为阿里云支持的语言代码
     * 
     * @param languageCode 标准语言代码
     * @return 阿里云语言代码
     */
    private String convertLanguageCode(String languageCode) {
        if (!StringUtils.hasText(languageCode)) {
            return "auto";
        }
        
        // 阿里云翻译支持的语言代码映射
        switch (languageCode.toLowerCase()) {
            case "zh":
            case "zh-cn":
                return "zh";
            case "zh-tw":
                return "zh-tw";
            case "en":
                return "en";
            case "ja":
                return "ja";
            case "ko":
                return "ko";
            case "fr":
                return "fr";
            case "de":
                return "de";
            case "es":
                return "es";
            case "ru":
                return "ru";
            case "ar":
                return "ar";
            case "pt":
                return "pt";
            case "it":
                return "it";
            case "th":
                return "th";
            case "vi":
                return "vi";
            default:
                return languageCode;
        }
    }
    
    /**
     * 将阿里云语言代码转换为标准语言代码
     * 
     * @param aliyunLanguageCode 阿里云语言代码
     * @return 标准语言代码
     */
    private String convertToStandardLanguageCode(String aliyunLanguageCode) {
        if (!StringUtils.hasText(aliyunLanguageCode)) {
            return "unknown";
        }
        
        // 基本上阿里云的语言代码与标准代码一致，直接返回
        return aliyunLanguageCode.toLowerCase();
    }

    @Override
    public String getEngineCode() {
        return "ALIBABA_CLOUD";
    }
}