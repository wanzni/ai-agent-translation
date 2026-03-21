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
 * 系统通知实体类
 * 
 * 存储系统级别的通知信息，包括公告、更新通知、系统消息等
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "system_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemNotification {

    /**
     * 通知ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知标题
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 通知内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 通知类型
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "notification_type", nullable = false)
    @Builder.Default
    private NotificationType notificationType = NotificationType.SYSTEM;

    /**
     * 优先级
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    /**
     * 目标用户（JSON格式）
     */
    @Column(name = "target_users", columnDefinition = "TEXT")
    private String targetUsers;

    /**
     * 是否全局通知
     */
    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;

    /**
     * 是否启用
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 发布时间
     */
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    /**
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

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
     * 通知类型枚举
     */
    public enum NotificationType {
        SYSTEM("系统通知"),
        TRANSLATION("翻译相关"),
        QUALITY("质量评估"),
        TERMINOLOGY("术语库"),
        MAINTENANCE("系统维护"),
        UPDATE("版本更新"),
        SECURITY("安全提醒");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW("低"),
        NORMAL("普通"),
        HIGH("高"),
        URGENT("紧急");

        private final String description;

        Priority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 检查通知是否已发布
     * 
     * @return 是否已发布
     */
    public boolean isPublished() {
        return this.publishTime != null && LocalDateTime.now().isAfter(this.publishTime);
    }

    /**
     * 检查通知是否已过期
     * 
     * @return 是否已过期
     */
    public boolean isExpired() {
        return this.expireTime != null && LocalDateTime.now().isAfter(this.expireTime);
    }

    /**
     * 检查通知是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return this.isActive && isPublished() && !isExpired();
    }

    /**
     * 发布通知
     */
    public void publish() {
        this.publishTime = LocalDateTime.now();
        this.isActive = true;
    }

    /**
     * 取消发布通知
     */
    public void unpublish() {
        this.isActive = false;
    }

    /**
     * 设置过期时间
     * 
     * @param days 天数
     */
    public void setExpireInDays(int days) {
        this.expireTime = LocalDateTime.now().plusDays(days);
    }

    /**
     * 设置过期时间
     * 
     * @param hours 小时数
     */
    public void setExpireInHours(int hours) {
        this.expireTime = LocalDateTime.now().plusHours(hours);
    }
}

