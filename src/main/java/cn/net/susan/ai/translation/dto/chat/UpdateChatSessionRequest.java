package cn.net.susan.ai.translation.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Size;

/**
 * 更新聊天会话请求DTO
 * 
 * @author 苏三
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateChatSessionRequest {
    
    @Size(max = 100, message = "会话名称长度不能超过100个字符")
    private String sessionName;
    
    private String sourceLanguage;
    private String targetLanguage;
    private String description;
}