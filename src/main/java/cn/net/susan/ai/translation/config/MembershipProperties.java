package cn.net.susan.ai.translation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 会员系统相关配置属性
 *
 * <p>该类用于映射和管理与会员制度相关的配置项，
 * 例如每月配额、订阅奖励等，这些配置项通常定义在 `application.yml` 文件中。
 *
 * @author 苏三
 * @version 1.0.0
 * @since 2025-11-21
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.membership")
public class MembershipProperties {

    /** 每月会员配额（点数），例如 5000 */
    private long monthlyQuota = 5000L;

    /** 会员开通时赠送的点数，例如 5000 */
    private long subscribeBonusPoints = 5000L;
}