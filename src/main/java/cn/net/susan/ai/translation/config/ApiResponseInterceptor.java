package cn.net.susan.ai.translation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API响应拦截器
 * 用于记录API请求日志
 * 
 * @author 苏三
 * @version 1.0
 * @since 2024-01-15
 */
@Slf4j
@Component
public class ApiResponseInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录API请求日志
        if (isApiRequest(request.getRequestURI())) {
            log.debug("API请求开始: {} {}", request.getMethod(), request.getRequestURI());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 记录API请求完成日志
        if (isApiRequest(request.getRequestURI())) {
            log.debug("API请求完成: {} {} - 状态码: {}", 
                request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }

    /**
     * 判断是否为API请求
     */
    private boolean isApiRequest(String requestURI) {
        return requestURI.startsWith("/api/");
    }
}
