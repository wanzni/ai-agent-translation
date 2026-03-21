package cn.net.wanzni.ai.translation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 翻译引擎配置实体类
 * 
 * 存储翻译引擎的配置信息，包括API端点、密钥、支持的语言等
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "translation_engines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationEngine {

    /**
     * 引擎ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 引擎名称
     */
    @Column(name = "engine_name", nullable = false, length = 50)
    private String engineName;

    /**
     * 引擎代码
     */
    @Column(name = "engine_code", nullable = false, unique = true, length = 50)
    private String engineCode;

    /**
     * API端点
     */
    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    /**
     * API密钥
     */
    @Column(name = "api_key", length = 255)
    private String apiKey;

    /**
     * 是否启用
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 优先级
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    /**
     * 每分钟最大请求数
     */
    @Column(name = "max_requests_per_minute")
    private Integer maxRequestsPerMinute;

    /**
     * 支持的语言列表（JSON格式）
     */
    @Column(name = "supported_languages", columnDefinition = "TEXT")
    private String supportedLanguages;

    /**
     * 配置参数（JSON格式）
     */
    @Column(name = "config_params", columnDefinition = "TEXT")
    private String configParams;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 检查引擎是否可用
     * 
     * @return 是否可用
     */
    public boolean isAvailable() {
        return this.isActive && this.apiKey != null && !this.apiKey.trim().isEmpty();
    }

    /**
     * 检查是否支持指定语言
     * 
     * @param languageCode 语言代码
     * @return 是否支持
     */
    public boolean supportsLanguage(String languageCode) {
        if (this.supportedLanguages == null || this.supportedLanguages.trim().isEmpty()) {
            return false;
        }
        // 简单的字符串包含检查，实际项目中可以使用JSON解析
        return this.supportedLanguages.contains(languageCode);
    }

    /**
     * 获取配置参数
     * 
     * @param key 参数键
     * @return 参数值
     */
    public String getConfigParam(String key) {
        if (this.configParams == null || this.configParams.trim().isEmpty()) {
            return null;
        }
        // 简单的字符串解析，实际项目中可以使用JSON解析
        // 这里只是示例，实际应该使用JSON库
        return null;
    }

    /**
     * 设置配置参数
     * 
     * @param key 参数键
     * @param value 参数值
     */
    public void setConfigParam(String key, String value) {
        // 简单的字符串操作，实际项目中应该使用JSON库
        // 这里只是示例
    }
}

