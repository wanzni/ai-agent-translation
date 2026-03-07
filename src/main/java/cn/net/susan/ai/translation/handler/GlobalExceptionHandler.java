package cn.net.susan.ai.translation.handler;

import cn.net.susan.ai.translation.dto.ApiResponse;
import cn.net.susan.ai.translation.exception.InsufficientPointsException;
import cn.net.susan.ai.translation.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * 统一处理各种异常，返回标准化的错误响应
 * 只处理API接口异常，不影响页面异常处理
 * 
 * @author 苏三
 * @version 1.0
 * @since 2024-01-15
 */
// 只拦截@RestController（API控制器），不影响页面@Controller
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        logger.warn("参数验证失败: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "参数验证失败", 
                "VALIDATION_ERROR", 
                errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        logger.warn("数据绑定失败: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "数据绑定失败", 
                "BIND_ERROR", 
                errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理方法参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        logger.warn("参数类型不匹配: {}", ex.getMessage());
        
        String errorMessage = String.format("参数 '%s' 的值 '%s' 无法转换为 %s 类型", 
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        
        ApiResponse<String> response = ApiResponse.error(
                "参数类型不匹配", 
                "TYPE_MISMATCH", 
                errorMessage
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        logger.warn("文件上传大小超限: {}", ex.getMessage());
        
        ApiResponse<String> response = ApiResponse.error(
                "文件上传大小超限", 
                "FILE_TOO_LARGE", 
                "上传的文件大小超过了限制"
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        logger.warn("非法参数: {}", ex.getMessage());
        
        ApiResponse<String> response = ApiResponse.error(
                "非法参数", 
                "ILLEGAL_ARGUMENT", 
                ex.getMessage()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        
        logger.warn("非法状态: {}", ex.getMessage());
        
        ApiResponse<String> response = ApiResponse.error(
                "非法状态", 
                "ILLEGAL_STATE", 
                ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<String>> handleNullPointerException(
            NullPointerException ex, HttpServletRequest request) {
        
        logger.error("空指针异常: {}", ex.getMessage(), ex);
        
        ApiResponse<String> response = ApiResponse.error(
                "系统内部错误", 
                "NULL_POINTER", 
                "发生了空指针异常，请联系管理员"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        logger.error("运行时异常: {}", ex.getMessage(), ex);
        
        ApiResponse<String> response = ApiResponse.error(
                "系统内部错误", 
                "RUNTIME_ERROR", 
                "系统发生运行时异常，请联系管理员"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        logger.error("未知异常: {}", ex.getMessage(), ex);
        
        ApiResponse<String> response = ApiResponse.error(
                "系统内部错误", 
                "INTERNAL_ERROR", 
                "系统发生未知异常，请联系管理员"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理点数不足异常
     */
    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<ApiResponse<String>> handleInsufficientPointsException(
            InsufficientPointsException ex, HttpServletRequest request) {
        // 增强参数日志：请求方法、路径、查询参数、关键标识字段
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String userId = (UserContext.getUserId() == null) ? null : String.valueOf(UserContext.getUserId());
        String taskId = request.getParameter("id");
        String engine = request.getParameter("translationEngine");
        String targetLanguage = request.getParameter("targetLanguage");
        String translationType = request.getParameter("translationType");
        logger.warn("点数不足: {} | method={}, uri={}, query={}, userId={}, taskId={}, engine={}, targetLanguage={}, translationType={}",
                ex.getMessage(), method, uri, query, userId, taskId, engine, targetLanguage, translationType);
        ApiResponse<String> response = ApiResponse.error(
                "点数不足，无法完成当前操作",
                "INSUFFICIENT_POINTS",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }
}