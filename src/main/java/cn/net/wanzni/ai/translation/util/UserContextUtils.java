package cn.net.wanzni.ai.translation.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户上下文工具类
 * 
 * 用于从HTTP请求中获取当前登录用户ID
 * 
 * @version 1.0.0
 */
@Slf4j
public class UserContextUtils {

    /**
     * 从请求头中获取用户ID
     * 支持的请求头：
     * 1. X-User-Id: 用户ID
     * 2. Authorization: Bearer token（需要解析token获取用户ID）
     * 
     * @param request HTTP请求
     * @return 用户ID，如果不存在则返回null
     */
    public static String getUserIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // 优先从请求头 X-User-Id 获取
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.trim().isEmpty()) {
            return userId.trim();
        }

        // 尝试从 Authorization: Bearer <userId> 获取（简化解析）
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.trim().isEmpty()) {
            String auth = authorization.trim();
            if (auth.toLowerCase().startsWith("bearer ")) {
                String token = auth.substring(7).trim();
                if (!token.isEmpty()) {
                    return token;
                }
            }
        }
        
        // 从请求参数中获取（用于GET请求）
        userId = request.getParameter("userId");
        if (userId != null && !userId.trim().isEmpty()) {
            return userId.trim();
        }
        
        return null;
    }

    /**
     * 从当前请求上下文中获取用户ID
     * 
     * @return 用户ID，如果不存在则返回null
     */
    public static String getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getUserIdFromRequest(request);
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
        }
        return null;
    }

    /**
     * 验证用户ID是否有效
     * 不允许使用 "home-user" 等默认值
     * 
     * @param userId 用户ID
     * @return 是否有效
     */
    public static boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        String trimmed = userId.trim();
        // 不允许使用默认值
        if ("home-user".equals(trimmed) || "888".equals(trimmed)) {
            return false;
        }
        return true;
    }

    /**
     * 获取当前用户ID，如果无效则返回null
     * 
     * @return 有效的用户ID，如果不存在或无效则返回null
     */
    public static String getCurrentValidUserId() {
        String userId = getCurrentUserId();
        if (isValidUserId(userId)) {
            return userId;
        }
        return null;
    }

    /**
     * 安全地解析用户ID，如果无效则返回null
     *
     * @param userId 用户ID
     * @return 有效的用户ID或null
     */
    public static Long safeParseUserId(String userId) {
        if (!isValidUserId(userId)) {
            return null;
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userId);
            return null;
        }
    }
}
