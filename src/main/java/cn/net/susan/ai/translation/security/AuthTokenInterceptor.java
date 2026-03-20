package cn.net.susan.ai.translation.security;

import cn.net.susan.ai.translation.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthTokenInterceptor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // Allow static and auth endpoints
        if (uri.startsWith("/static/") || uri.startsWith("/css/") || uri.startsWith("/js/") ||
                uri.startsWith("/images/") || uri.startsWith("/login") || uri.startsWith("/api/auth/") ||
                uri.startsWith("/api/agent/")) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            unauthorized(response, "Missing token");
            return false;
        }

        try {
            Claims claims = JwtUtil.parseToken(token);
            if (JwtUtil.isExpired(claims)) {
                unauthorized(response, "Token expired");
                return false;
            }
            String redisKey = "auth:token:" + token;
            String userJson = redisTemplate.opsForValue().get(redisKey);
            if (userJson == null) {
                unauthorized(response, "Session expired");
                return false;
            }
            User user = objectMapper.readValue(userJson.getBytes(StandardCharsets.UTF_8), User.class);
            UserContext.set(user);
            return true;
        } catch (Exception e) {
            unauthorized(response, "Invalid token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request){
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")){
            return header.substring(7);
        }
        // fallback to cookie
        if (request.getCookies() != null){
            for (var c : request.getCookies()){
                if ("auth_token".equals(c.getName())){
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}