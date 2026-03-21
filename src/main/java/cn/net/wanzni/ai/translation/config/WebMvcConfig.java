package cn.net.wanzni.ai.translation.config;

import cn.net.wanzni.ai.translation.security.AuthTokenInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC 配置类
 *
 * <p>该类负责配置 Spring Web MVC 的相关功能，特别是拦截器的注册。
 * 它通过实现 {@link WebMvcConfigurer} 接口，来自定义 MVC 的行为。
 *
 * <p>在此配置中，主要注册了 {@link AuthTokenInterceptor}，用于对 API 请求进行认证和授权。
 * 拦截器会检查请求头中的认证令牌，并验证其有效性，以保护需要授权访问的 API 接口。
 *
 * @version 1.0.0
 * @since 2024-07-28
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 添加拦截器到 Spring MVC 拦截器链
     *
     * <p>此方法注册了 {@link AuthTokenInterceptor}，并配置了其拦截和排除的 URL 模式。
     * 拦截器将应用于所有 `/api/**` 路径下的请求，但会排除认证、支付、静态资源等公开访问的接口。
     *
     * @param registry 拦截器注册表，用于添加和配置拦截器。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthTokenInterceptor(stringRedisTemplate, objectMapper))
                // 仅拦截 API 接口，放行页面与静态资源
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 认证相关接口放行（登录、发送验证码、注册等）
                        "/api/auth/**",
                        // 支付流程相关接口放行：用于手机端扫码确认与PC端轮询无需登录
                        "/api/payment/**",
                        "/api/membership/order",
                        "/api/membership/order/**",
                        // 静态资源
                        "/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**",
                        // 公开资源与错误
                        "/favicon.ico", "/error"
                );
    }
}