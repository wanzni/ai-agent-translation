package cn.net.susan.ai.translation.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 聊天会话响应DTO
 * 
 * @author 苏三
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {
    
    private String sessionId;
    private String sessionName;
    private String sourceLanguage;
    private String targetLanguage;
    private String createdBy;
    private LocalDateTime createdAt;
    private String status;
    private int participantCount;
    private int messageCount;
    private LocalDateTime lastActivity;
    private String description;
}