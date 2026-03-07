package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查响应
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 服务名
     */
    private String service;

    /**
     * 版本号
     */
    private String version;
}