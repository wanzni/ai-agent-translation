package cn.net.susan.ai.translation.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注销响应
 *
 * @author sushan
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutResponse {
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 消息
     */
    private String message;
}