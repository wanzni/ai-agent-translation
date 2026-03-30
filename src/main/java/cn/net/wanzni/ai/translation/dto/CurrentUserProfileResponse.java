package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginTime;
}
