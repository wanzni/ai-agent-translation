package cn.net.susan.ai.translation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 处理浏览器默认的 /favicon.ico 请求，避免静态资源缺失异常。
 * 如果需要显示实际站点图标，请将文件放置在
 * src/main/resources/static/favicon.ico 并移除此控制器。
 */
@Controller
public class FaviconController {

    /**
     * 返回 204 No Content，消除 NoResourceFoundException 日志噪音。
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}