package cn.net.wanzni.ai.translation.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean isJson = request.getContentType() != null
                && request.getContentType().toLowerCase().contains("application/json");

        HttpServletRequest req = request;
        if (isJson) {
            // 限制缓存的请求体大小，避免超大请求导致内存占用
            req = new ContentCachingRequestWrapper(request, 16 * 1024);
        }

        try {
            filterChain.doFilter(req, response);
        } finally {
            if (isJson && req instanceof ContentCachingRequestWrapper wrapper) {
                String body = new String(wrapper.getContentAsByteArray(),
                        wrapper.getCharacterEncoding() != null ? wrapper.getCharacterEncoding() : StandardCharsets.UTF_8.name());
                String method = request.getMethod();
                String uri = request.getRequestURI();
                String query = request.getQueryString();
                log.info("请求体日志: method={}, uri={}, query={}, body={}", method, uri, query, truncate(body, 2000));
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : (s.substring(0, max) + "...(truncated " + s.length() + ")");
    }
}