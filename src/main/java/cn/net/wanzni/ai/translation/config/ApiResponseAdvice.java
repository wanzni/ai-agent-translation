package cn.net.wanzni.ai.translation.config;

import cn.net.wanzni.ai.translation.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * API响应统一包装器
 * 
 * 自动包装所有API接口的返回值，统一返回格式
 * 只处理API接口，不影响页面返回
 * 
 * @version 1.0
 * @since 2024-01-15
 */
@ControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ApiResponseAdvice.class);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 判断是否需要处理响应
     * 只处理API接口（/api/**），不处理页面返回
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法是否在Controller中
        if (returnType.getDeclaringClass().getPackage().getName().contains("controller")) {
            // 检查是否是API接口（通过方法上的注解或路径判断）
            String methodName = returnType.getMethod().getName();
            String className = returnType.getDeclaringClass().getSimpleName();
            
            // 排除PageController，只处理API Controller
            if (!className.equals("PageController")) {
                logger.debug("API响应包装器将处理: {}.{}", className, methodName);
                return true;
            }
        }
        return false;
    }

    /**
     * 包装响应体
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // SSE 或二进制等特殊类型不做统一包装
        try {
            if (body instanceof org.springframework.web.servlet.mvc.method.annotation.SseEmitter) {
                // 事件流需保持原始 text/event-stream
                return body;
            }
            if (MediaType.TEXT_EVENT_STREAM.equals(selectedContentType)) {
                return body;
            }
            if (body instanceof byte[]) {
                // 二进制内容（下载）维持原样
                return body;
            }
        } catch (Exception ignore) {}
        
        // 如果已经是ApiResponse类型，直接返回
        if (body instanceof ApiResponse) {
            logger.debug("响应已经是ApiResponse类型，直接返回");
            return body;
        }

        // 如果是ResponseEntity类型，需要特殊处理
        if (body instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) body;
            Object responseBody = responseEntity.getBody();
            MediaType contentType = responseEntity.getHeaders().getContentType();
            // 不包装 SSE 或二进制下载
            if (responseBody instanceof org.springframework.web.servlet.mvc.method.annotation.SseEmitter
                    || MediaType.TEXT_EVENT_STREAM.equals(contentType)
                    || responseBody instanceof byte[]) {
                return body;
            }
            
            // 如果ResponseEntity的body已经是ApiResponse，直接返回
            if (responseBody instanceof ApiResponse) {
                logger.debug("ResponseEntity的body已经是ApiResponse类型，直接返回");
                return body;
            }
            
            // 包装ResponseEntity的body
            ApiResponse<?> apiResponse = ApiResponse.success(responseBody);
            return ResponseEntity.status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .body(apiResponse);
        }

        // 包装普通对象
        ApiResponse<Object> apiResponse = ApiResponse.success(body);
        logger.debug("包装响应体为ApiResponse: {}", apiResponse);
        
        return apiResponse;
    }
}