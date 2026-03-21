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
 * 术语库条目实体类
 * 
 * 存储专业术语的翻译对照，支持多语言术语管理
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "terminology_entries", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_term", "source_language", "target_language"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyEntry {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 源术语
     */
    @Column(name = "source_term", nullable = false, length = 500)
    private String sourceTerm;

    /**
     * 目标翻译
     */
    @Column(name = "target_term", nullable = false, length = 500)
    private String targetTerm;

    /**
     * 源语言代码
     */
    @Column(name = "source_language", nullable = false, length = 10)
    private String sourceLanguage;

    /**
     * 目标语言代码
     */
    @Column(name = "target_language", nullable = false, length = 10)
    private String targetLanguage;

    /**
     * 术语分类
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TerminologyCategory category;

    /**
     * 领域标签
     */
    @Column(name = "domain", length = 100)
    private String domain;

    /**
     * 备注信息
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * 使用频率
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * 是否启用
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 创建者ID
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 用户ID（用于区分不同用户的术语库）
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

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
     * 术语分类枚举
     */
    public enum TerminologyCategory {
        TECHNOLOGY("技术术语"),
        BUSINESS("商业术语"),
        MEDICAL("医学术语"),
        LEGAL("法律术语"),
        FINANCE("金融术语"),
        EDUCATION("教育术语"),
        SCIENCE("科学术语"),
        GENERAL("通用术语");

        private final String description;

        TerminologyCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }
}