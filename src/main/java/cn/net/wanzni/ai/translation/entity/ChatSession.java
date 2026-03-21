package cn.net.wanzni.ai.translation.entity;

import cn.net.wanzni.ai.translation.enums.SessionStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 * 
 * 存储实时对话翻译的会话信息和消息记录
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话唯一标识
     */
    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    /**
     * 用户A的ID
     */
    @Column(name = "user_a_id", length = 100)
    private String userAId;

    /**
     * 用户B的ID
     */
    @Column(name = "user_b_id", length = 100)
    private String userBId;

    /**
     * 用户A的语言
     */
    @Column(name = "user_a_language", nullable = false, length = 10)
    private String userALanguage;

    /**
     * 用户B的语言
     */
    @Column(name = "user_b_language", nullable = false, length = 10)
    private String userBLanguage;

    /**
     * 会话标题
     */
    @Column(name = "session_title", length = 200)
    private String sessionTitle;

    /**
     * 会话状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SessionStatusEnum status = SessionStatusEnum.ACTIVE;

    /**
     * 消息总数
     */
    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;

    /**
     * 是否启用自动翻译
     */
    @Column(name = "auto_translate")
    @Builder.Default
    private Boolean autoTranslate = true;

    /**
     * 是否使用术语库
     */
    @Column(name = "use_terminology")
    @Builder.Default
    private Boolean useTerminology = false;

    /**
     * 最后活跃时间
     */
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

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
     * 会话结束时间
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * 增加消息计数
     */
    public void incrementMessageCount() {
        this.messageCount = (this.messageCount == null ? 0 : this.messageCount) + 1;
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 结束会话
     */
    public void endSession() {
        this.status = SessionStatusEnum.ENDED;
        this.endedAt = LocalDateTime.now();
    }
}