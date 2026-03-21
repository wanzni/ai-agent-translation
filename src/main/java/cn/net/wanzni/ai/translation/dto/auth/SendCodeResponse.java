package cn.net.wanzni.ai.translation.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码响应
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendCodeResponse {
    /**
     * 是否已发送
     */
    private boolean sent;
    /**
     * 冷却时间（秒）
     */
    private int cooldownSeconds;
    /**
     * 过期时间（秒）
     */
    private int expiresInSeconds;
    /**
     * 消息
     */
    private String message;
}