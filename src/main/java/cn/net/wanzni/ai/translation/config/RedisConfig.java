package cn.net.wanzni.ai.translation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redis 配置类
 *
 * <p>该类负责配置和初始化与 Redis 的连接。
 * 它从 Spring 环境中读取 Redis 的主机、端口、密码、数据库索引和超时等配置，
 * 并创建一个 {@link RedisConnectionFactory} Bean 和一个 {@link StringRedisTemplate} Bean，
 * 以便在应用程序中方便地与 Redis 进行交互。
 *
 * <p>配置支持从 `spring.data.redis.*` 和 `spring.redis.*` 两种前缀读取属性，
 * 并提供了默认值，以增加配置的灵活性。
 *
 * @version 1.0.0
 * @since 2025-11-21
 */
@Configuration
public class RedisConfig {

    /** Redis 服务器主机地址 */
    @Value("${spring.data.redis.host:${spring.redis.host:localhost}}")
    private String host;

    /** Redis 服务器端口 */
    @Value("${spring.data.redis.port:${spring.redis.port:6379}}")
    private int port;

    /** Redis 服务器密码 */
    @Value("${spring.data.redis.password:${spring.redis.password:}}")
    private String password;

    /** Redis 数据库索引 */
    @Value("${spring.data.redis.database:${spring.redis.database:0}}")
    private int database;

    /** Redis 命令超时时间 */
    @Value("${spring.data.redis.timeout:${spring.redis.timeout:5000ms}}")
    private Duration timeout;

    /**
     * 创建 Redis 连接工厂 Bean
     *
     * <p>此方法使用 Lettuce 作为 Redis 客户端，并根据配置构建一个
     * {@link RedisConnectionFactory}。它支持独立模式的 Redis 连接，
     * 并可配置密码和数据库索引。
     *
     * @return 配置完成的 {@link RedisConnectionFactory} 实例。
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        if (StringUtils.hasText(password)) {
            standalone.setPassword(RedisPassword.of(password));
        }
        standalone.setDatabase(database);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .build();

        return new LettuceConnectionFactory(standalone, clientConfig);
    }

    /**
     * 创建 StringRedisTemplate Bean
     *
     * <p>提供一个专门用于操作字符串类型键值的 Redis 模板。
     * 它是与 Redis 交互的便捷高级抽象。
     *
     * @param factory Redis 连接工厂
     * @return {@link StringRedisTemplate} 实例。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory){
        return new StringRedisTemplate(factory);
    }
}