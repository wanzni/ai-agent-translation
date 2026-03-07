package cn.net.susan.ai.translation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * 配置CORS、静态资源处理、拦截器等Web相关设置
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApiResponseInterceptor apiResponseInterceptor;

    /**
     * 配置CORS跨域访问
     * 
     * @param registry CORS注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理
     * 
     * @param registry 资源处理注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 上传文件访问路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    /**
     * 配置拦截器
     * 
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只拦截API接口，不影响页面返回
        registry.addInterceptor(apiResponseInterceptor)
                .addPathPatterns("/api/**")  // 只拦截API接口
                .excludePathPatterns(
                    "/",                    // 首页
                    "/translate",           // 翻译页面
                    "/translate/batch",     // 批量翻译页面
                    "/document",            // 文档翻译页面
                    "/terminology",         // 术语库页面
                    "/chat",               // 聊天翻译页面
                    "/history",            // 翻译历史页面
                    "/quality",            // 质量评估页面
                    "/statistics",         // 统计分析页面
                    "/settings",           // 系统设置页面
                    "/profile",            // 个人资料页面
                    "/api-keys",           // API密钥管理页面
                    "/help",               // 使用帮助页面
                    "/api-docs",           // API文档页面
                    "/feedback",           // 意见反馈页面
                    "/about",              // 关于我们页面
                    "/contact",            // 联系我们页面
                    "/static/**",          // 静态资源
                    "/uploads/**",         // 上传文件
                    "/h2-console/**",      // H2控制台
                    "/error"               // 错误页面
                );
    }

    /**
     * 兜底的页面视图映射
     * 当@Controller未生效或存在路径冲突时，保证关键页面可访问
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/profile").setViewName("profile");
    }
}