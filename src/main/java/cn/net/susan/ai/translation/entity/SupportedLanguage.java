package cn.net.susan.ai.translation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 支持语言实体类
 * 
 * 存储系统支持的语言信息，包括语言代码、名称、本地名称等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "supported_languages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportedLanguage {

    /**
     * 语言ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 语言代码
     */
    @Column(name = "language_code", nullable = false, unique = true, length = 10)
    private String languageCode;

    /**
     * 语言名称
     */
    @Column(name = "language_name", nullable = false, length = 100)
    private String languageName;

    /**
     * 本地语言名称
     */
    @Column(name = "native_name", length = 100)
    private String nativeName;

    /**
     * 是否启用
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

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
     * 获取显示名称
     * 优先使用本地名称，如果没有则使用语言名称
     * 
     * @return 显示名称
     */
    public String getDisplayName() {
        return this.nativeName != null && !this.nativeName.trim().isEmpty() 
            ? this.nativeName 
            : this.languageName;
    }

    /**
     * 检查语言是否可用
     * 
     * @return 是否可用
     */
    public boolean isAvailable() {
        return this.isActive;
    }

    /**
     * 获取语言代码（大写）
     * 
     * @return 大写语言代码
     */
    public String getLanguageCodeUpperCase() {
        return this.languageCode != null ? this.languageCode.toUpperCase() : null;
    }

    /**
     * 获取语言代码（小写）
     * 
     * @return 小写语言代码
     */
    public String getLanguageCodeLowerCase() {
        return this.languageCode != null ? this.languageCode.toLowerCase() : null;
    }
}

