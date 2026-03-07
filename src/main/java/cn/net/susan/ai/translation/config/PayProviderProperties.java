package cn.net.susan.ai.translation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付提供商配置属性
 *
 * <p>该类作为支付相关配置的根节点，通过 `@EnableConfigurationProperties`
 * 激活并引入嵌套的支付宝 ({@link Alipay}) 和微信支付 ({@link WeChat}) 配置类。
 * 这种结构使得支付配置在 `application.yml` 中更具组织性。
 *
 * @author 苏三
 * @version 1.0.0
 * @since 2025-11-21
 */
@Configuration
@EnableConfigurationProperties({PayProviderProperties.Alipay.class, PayProviderProperties.WeChat.class})
public class PayProviderProperties {

    /**
     * 支付宝配置属性
     *
     * <p>映射 `pay.alipay.*` 下的所有配置项。
     */
    @Data
    @ConfigurationProperties(prefix = "pay.alipay")
    public static class Alipay {
        /** 是否启用支付宝支付 */
        private boolean enabled = false;
        /** 是否使用沙箱环境 */
        private boolean sandbox = true;
        /** 支付宝应用ID */
        private String appId;
        /** 商户私钥 */
        private String merchantPrivateKey;
        /** 支付宝公钥 */
        private String alipayPublicKey;
        /** 异步通知回调地址 */
        private String notifyUrl;
    }

    /**
     * 微信支付配置属性
     *
     * <p>映射 `pay.wechat.*` 下的所有配置项。
     */
    @Data
    @ConfigurationProperties(prefix = "pay.wechat")
    public static class WeChat {
        /** 是否启用微信支付 */
        private boolean enabled = false;
        /** 微信应用ID */
        private String appId;
        /** 微信商户号 */
        private String mchid;
        /** 商户证书序列号 */
        private String merchantSerialNumber;
        /** APIv3 密钥 */
        private String apiV3Key;
        /** 商户私钥文件路径 */
        private String privateKeyPath;
        /** 异步通知回调地址 */
        private String notifyUrl;
    }
}