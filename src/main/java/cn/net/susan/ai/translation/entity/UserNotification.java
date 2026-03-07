package cn.net.susan.ai.translation.entity;

import cn.net.susan.ai.translation.enums.NotificationTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户通知实体类
 * 
 * 存储用户个人通知信息，包括系统通知、个人消息等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "user_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {

    /**
     * 用户通知ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 系统通知ID
     */
    @Column(name = "notification_id")
    private Long notificationId;

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
    private NotificationTypeEnum notificationType = NotificationTypeEnum.SYSTEM;

    /**
     * 是否已读
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 阅读时间
     */
    @Column(name = "read_time")
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
     * 标记为已读
     */
    public void markAsRead() {
        this.isRead = true;
        this.readTime = LocalDateTime.now();
    }

    /**
     * 标记为未读
     */
    public void markAsUnread() {
        this.isRead = false;
        this.readTime = null;
    }

    /**
     * 检查是否为未读通知
     * 
     * @return 是否为未读
     */
    public boolean isUnread() {
        return !this.isRead;
    }

    /**
     * 获取通知类型描述
     * 
     * @return 通知类型描述
     */
    public String getNotificationTypeDescription() {
        return this.notificationType.getDesc();
    }

    /**
     * 检查是否为系统通知
     * 
     * @return 是否为系统通知
     */
    public boolean isSystemNotification() {
        return this.notificationType == NotificationTypeEnum.SYSTEM;
    }

    /**
     * 检查是否为翻译相关通知
     * 
     * @return 是否为翻译相关通知
     */
    public boolean isTranslationNotification() {
        return this.notificationType == NotificationTypeEnum.TRANSLATION;
    }

    /**
     * 检查是否为质量评估通知
     * 
     * @return 是否为质量评估通知
     */
    public boolean isQualityNotification() {
        return this.notificationType == NotificationTypeEnum.QUALITY;
    }

    /**
     * 检查是否为术语库通知
     * 
     * @return 是否为术语库通知
     */
    public boolean isTerminologyNotification() {
        return this.notificationType == NotificationTypeEnum.TERMINOLOGY;
    }
}

