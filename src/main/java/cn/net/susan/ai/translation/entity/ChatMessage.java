package cn.net.susan.ai.translation.entity;

import cn.net.susan.ai.translation.enums.MessageTypeEnum;
import cn.net.susan.ai.translation.enums.TranslationStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 
 * 存储实时对话翻译中的消息内容和翻译结果
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的会话ID
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * 发送者ID
     */
    @Column(name = "sender_id", nullable = false, length = 100)
    private Long senderId;

    /**
     * 接收者ID
     */
    @Column(name = "receiver_id", length = 100)
    private String receiverId;

    /**
     * 原始消息内容
     */
    @Column(name = "original_message", nullable = false, columnDefinition = "TEXT")
    private String originalMessage;

    /**
     * 翻译后的消息内容
     */
    @Column(name = "translated_message", columnDefinition = "TEXT")
    private String translatedMessage;

    /**
     * 源语言代码
     */
    @Column(name = "source_language", nullable = false, length = 10)
    private String sourceLanguage;

    /**
     * 目标语言代码
     */
    @Column(name = "target_language", length = 10)
    private String targetLanguage;

    /**
     * 消息类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageTypeEnum messageType = MessageTypeEnum.TEXT;

    /**
     * 翻译状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false)
    @Builder.Default
    private TranslationStatusEnum translationStatus = TranslationStatusEnum.PENDING;

    /**
     * 翻译引擎
     */
    @Column(name = "translation_engine", length = 50)
    private String translationEngine;

    /**
     * 翻译耗时（毫秒）
     */
    @Column(name = "translation_time")
    private Long translationTime;

    /**
     * 是否使用术语库
     */
    @Column(name = "use_terminology")
    @Builder.Default
    private Boolean useTerminology = false;

    /**
     * 消息序号（在会话中的顺序）
     */
    @Column(name = "message_sequence")
    private Integer messageSequence;

    /**
     * 是否已读
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 错误信息（如果翻译失败）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.isRead = true;
    }
}