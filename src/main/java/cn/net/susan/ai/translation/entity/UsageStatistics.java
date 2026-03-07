package cn.net.susan.ai.translation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 使用统计实体类
 * 
 * 存储用户使用统计信息，包括翻译次数、字符数、文档数等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "usage_statistics", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stat_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageStatistics {

    /**
     * 统计ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 统计日期
     */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /**
     * 翻译次数
     */
    @Column(name = "translation_count")
    @Builder.Default
    private Integer translationCount = 0;

    /**
     * 字符数
     */
    @Column(name = "character_count")
    @Builder.Default
    private Long characterCount = 0L;

    /**
     * 文档翻译次数
     */
    @Column(name = "document_count")
    @Builder.Default
    private Integer documentCount = 0;

    /**
     * 聊天消息数
     */
    @Column(name = "chat_message_count")
    @Builder.Default
    private Integer chatMessageCount = 0;

    /**
     * 术语库使用次数
     */
    @Column(name = "terminology_usage_count")
    @Builder.Default
    private Integer terminologyUsageCount = 0;

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
     * 增加翻译次数
     * 
     * @param count 增加的次数
     */
    public void incrementTranslationCount(int count) {
        this.translationCount = (this.translationCount == null ? 0 : this.translationCount) + count;
    }

    /**
     * 增加字符数
     * 
     * @param count 增加的字符数
     */
    public void incrementCharacterCount(long count) {
        this.characterCount = (this.characterCount == null ? 0L : this.characterCount) + count;
    }

    /**
     * 增加文档翻译次数
     * 
     * @param count 增加的次数
     */
    public void incrementDocumentCount(int count) {
        this.documentCount = (this.documentCount == null ? 0 : this.documentCount) + count;
    }

    /**
     * 增加聊天消息数
     * 
     * @param count 增加的消息数
     */
    public void incrementChatMessageCount(int count) {
        this.chatMessageCount = (this.chatMessageCount == null ? 0 : this.chatMessageCount) + count;
    }

    /**
     * 增加术语库使用次数
     * 
     * @param count 增加的次数
     */
    public void incrementTerminologyUsageCount(int count) {
        this.terminologyUsageCount = (this.terminologyUsageCount == null ? 0 : this.terminologyUsageCount) + count;
    }

    /**
     * 获取总使用次数
     * 
     * @return 总使用次数
     */
    public Integer getTotalUsageCount() {
        return (this.translationCount == null ? 0 : this.translationCount) +
               (this.documentCount == null ? 0 : this.documentCount) +
               (this.chatMessageCount == null ? 0 : this.chatMessageCount);
    }

    /**
     * 获取平均字符数
     * 
     * @return 平均字符数
     */
    public Double getAverageCharacterCount() {
        int totalCount = getTotalUsageCount();
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) (this.characterCount == null ? 0L : this.characterCount) / totalCount;
    }
}

