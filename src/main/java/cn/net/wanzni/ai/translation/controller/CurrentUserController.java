package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.CurrentUserProfileResponse;
import cn.net.wanzni.ai.translation.entity.User;
import cn.net.wanzni.ai.translation.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {

    @GetMapping("/me")
    public CurrentUserProfileResponse currentUser() {
        User user = UserContext.get();
        if (user == null) {
            throw new IllegalStateException("Current user is unavailable");
        }
        return CurrentUserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() == null ? null : user.getRole().name())
                .status(user.getStatus() == null ? null : user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }
}
