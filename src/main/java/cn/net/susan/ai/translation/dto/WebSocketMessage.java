package cn.net.susan.ai.translation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * WebSocket消息DTO
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 额外数据
     */
    private Object data;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        USER_JOINED("user_joined"),
        USER_LEFT("user_left"),
        MESSAGE_SENT("message_sent"),
        MESSAGE_TRANSLATED("message_translated"),
        ERROR("error"),
        SYSTEM_NOTIFICATION("system_notification");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 创建用户加入消息
     */
    public static WebSocketMessage userJoined(String sessionId, Long userId) {
        return WebSocketMessage.builder()
                .type(MessageType.USER_JOINED.getValue())
                .sessionId(sessionId)
                .userId(userId)
                .message("用户加入会话")
                .build();
    }

    /**
     * 创建用户离开消息
     */
    public static WebSocketMessage userLeft(String sessionId, Long userId) {
        return WebSocketMessage.builder()
                .type(MessageType.USER_LEFT.getValue())
                .sessionId(sessionId)
                .userId(userId)
                .message("用户离开会话")
                .build();
    }

    /**
     * 创建错误消息
     */
    public static WebSocketMessage error(String message) {
        return WebSocketMessage.builder()
                .type(MessageType.ERROR.getValue())
                .message(message)
                .build();
    }

    /**
     * 创建系统通知消息
     */
    public static WebSocketMessage systemNotification(String message, Object data) {
        return WebSocketMessage.builder()
                .type(MessageType.SYSTEM_NOTIFICATION.getValue())
                .message(message)
                .data(data)
                .build();
    }
}
