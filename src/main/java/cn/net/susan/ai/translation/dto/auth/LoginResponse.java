package cn.net.susan.ai.translation.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 *
 * @author sushan
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    /**
     * 用户信息
     */
    private AuthUserResponse user;
    /**
     * 令牌
     */
    private String token;
    /**
     * 过期时间
     */
    private Long expiresIn;
}