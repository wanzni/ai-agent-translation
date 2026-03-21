package cn.net.wanzni.ai.translation.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送聊天消息请求DTO
 * 
 * @since 1.0.0
 */
@Data
@Builder
public class SendChatMessageRequest {
    private String message;
    private Long userId;
    private String userName;

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotBlank(message = "发送者ID不能为空")
    private String senderId;

    private String receiverId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容过长")
    private String originalMessage;

    @NotBlank(message = "源语言不能为空")
    private String sourceLanguage;

    private String targetLanguage;

    private int messageType = 0; // 默认为TEXT

    private Boolean useTerminology;

    private String translationEngine;
}