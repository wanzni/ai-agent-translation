package cn.net.wanzni.ai.translation.config;

import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.factory.Factory;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * 支付提供商配置
 *
 * <p>该配置类负责根据 `application.yml` 中的设置，初始化和配置不同的支付提供商，
 * 如微信支付和支付宝。它使用条件化注解（`@ConditionalOnProperty`）来确保只有在
 * 明确启用并提供了必要配置时，才会创建相应的支付服务 Bean。
 *
 * @version 1.0.0
 * @since 2025-11-21
 */
@Configuration
@RequiredArgsConstructor
public class PayProviderConfig {

    private final PayProviderProperties.Alipay alipayProps;
    private final PayProviderProperties.WeChat wechatProps;

    /**
     * 配置并创建微信支付 NativePay 服务
     *
     * <p>仅在满足以下所有条件时，此 Bean 才会被创建：
     * <ul>
     *   <li>`pay.wechat.enabled` 设置为 `true`。</li>
     *   <li>微信支付所需的所有关键配置（如私钥路径、商户序列号、APIv3密钥和商户ID）均已提供。</li>
     * </ul>
     *
     * @return 配置完成的 {@link NativePayService} 实例，用于发起微信 Native 支付。
     */
    @Bean
    @ConditionalOnProperty(prefix = "pay.wechat", name = "enabled", havingValue = "true")
    @ConditionalOnExpression("'${pay.wechat.private-key-path:}'.length() > 0 && '${pay.wechat.merchant-serial-number:}'.length() > 0 && '${pay.wechat.api-v3-key:}'.length() > 0 && '${pay.wechat.mchid:}'.length() > 0")
    public NativePayService wechatNativePayService() {
        RSAAutoCertificateConfig wxConfig = new RSAAutoCertificateConfig.Builder()
                .merchantId(wechatProps.getMchid())
                .privateKeyFromPath(wechatProps.getPrivateKeyPath())
                .merchantSerialNumber(wechatProps.getMerchantSerialNumber())
                .apiV3Key(wechatProps.getApiV3Key())
                .build();
        return new NativePayService.Builder().config(wxConfig).build();
    }

    /**
     * 配置支付宝 EasySDK
     *
     * <p>仅在 `pay.alipay.enabled` 设置为 `true` 时，此 Bean 才会被创建。
     * 它会根据配置（沙箱环境或生产环境）设置正确的网关地址，并初始化全局的
     * {@link Factory} 实例，以便在应用各处使用支付宝支付功能。
     *
     * @return 支付宝 EasySDK 的 {@link Config} 对象。
     */
    @Bean
    @ConditionalOnProperty(prefix = "pay.alipay", name = "enabled", havingValue = "true")
    public Config alipayEasySdkConfig() {
        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = alipayProps.isSandbox() ? "openapi-sandbox.dl.alipaydev.com" : "openapi.alipay.com";
        config.signType = "RSA2";
        config.appId = alipayProps.getAppId();
        config.merchantPrivateKey = alipayProps.getMerchantPrivateKey();
        config.alipayPublicKey = alipayProps.getAlipayPublicKey();
        config.notifyUrl = alipayProps.getNotifyUrl();
        // 初始化全局工厂
        Factory.setOptions(config);
        return config;
    }
}