package cn.net.susan.ai.translation.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息响应DTO
 * 
 * @author 苏三
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    
    private String messageId;
    private String sessionId;
    private Long userId;
    private String userName;
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private LocalDateTime sentAt;
    private int messageType;
    private List<String> readBy;
}