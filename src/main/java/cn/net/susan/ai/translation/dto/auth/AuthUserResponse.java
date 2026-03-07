package cn.net.susan.ai.translation.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证用户信息响应
 *
 * @author sushan
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserResponse {
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像URL
     */
    private String avatarUrl;
    /**
     * 角色
     */
    private String role;
    /**
     * 状态
     */
    private String status;
}