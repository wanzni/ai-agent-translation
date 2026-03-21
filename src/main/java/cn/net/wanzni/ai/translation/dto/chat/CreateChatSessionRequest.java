package cn.net.wanzni.ai.translation.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建聊天会话请求DTO
 * 
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatSessionRequest {
    
    @NotBlank(message = "会话名称不能为空")
    @Size(max = 100, message = "会话名称长度不能超过100个字符")
    private String sessionName;
    
    @NotBlank(message = "源语言不能为空")
    private String sourceLanguage;
    
    @NotBlank(message = "目标语言不能为空")
    private String targetLanguage;
    
    @NotBlank(message = "创建者不能为空")
    private String createdBy;
    
    private String description;
}