package cn.net.wanzni.ai.translation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 计费点数相关配置属性
 *
 * <p>该类用于映射和管理与计费点数相关的配置项，
 * 例如文本翻译和文档翻译的固定扣点数。
 * 这些配置项通常定义在 `application.yml` 文件中。
 *
 * @version 1.0.0
 * @since 2025-11-21
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.points")
public class PointsProperties {

    /** 文本翻译固定扣点数，默认 1 点 */
    private long textDeduction = 1L;

    /** 文档翻译固定扣点数，默认 10 点 */
    private long documentDeduction = 10L;
}