package cn.net.susan.ai.translation.config;

import com.aliyun.alimt20181012.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 阿里云翻译服务配置类
 * 
 * @author 苏三
 * @version 1.0.0
 * @since 2024-01-15
 */
@Configuration
@ConfigurationProperties(prefix = "ai.aliyun")
@Data
public class AliyunTranslationConfig {
    
    /**
     * 阿里云访问密钥ID
     */
    private String accessKeyId;
    
    /**
     * 阿里云访问密钥Secret
     */
    private String accessKeySecret;
    
    /**
     * 服务区域
     */
    private String region = "cn-hangzhou";
    
    /**
     * 服务端点
     */
    private String endpoint = "mt.cn-hangzhou.aliyuncs.com";
    
    /**
     * 创建阿里云翻译客户端Bean
     * 
     * @return 阿里云翻译客户端
     * @throws Exception 配置异常
     */
    @Bean
    public Client aliyunTranslationClient() throws Exception {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new IllegalArgumentException("阿里云翻译服务配置不完整，请检查access-key-id和access-key-secret配置");
        }
        
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setRegionId(region)
                .setEndpoint(endpoint);
        
        return new Client(config);
    }
}