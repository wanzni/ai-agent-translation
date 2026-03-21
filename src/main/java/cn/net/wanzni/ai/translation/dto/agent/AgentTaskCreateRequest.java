package cn.net.wanzni.ai.translation.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskCreateRequest {

    private Long userId;

    @NotBlank(message = "Source text cannot be empty")
    @Size(max = 50000, message = "Source text cannot exceed 50000 characters")
    private String sourceText;

    @NotBlank(message = "Source language cannot be empty")
    @Size(max = 16, message = "Source language is too long")
    private String sourceLanguage;

    @NotBlank(message = "Target language cannot be empty")
    @Size(max = 16, message = "Target language is too long")
    private String targetLanguage;

    @Size(max = 100, message = "Domain is too long")
    private String domain;

    @Size(max = 64, message = "Business type is too long")
    private String bizType;

    @Size(max = 64, message = "Business id is too long")
    private String bizId;
}
