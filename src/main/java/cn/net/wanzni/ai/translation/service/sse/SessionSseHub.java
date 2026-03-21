package cn.net.wanzni.ai.translation.service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class SessionSseHub {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public SseEmitter register(String sessionId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignore) {}

        scheduler.scheduleAtFixedRate(() -> {
            SseEmitter em = emitters.get(sessionId);
            if (em == null) return;
            try {
                em.send(SseEmitter.event().name("ping").data("keepalive"));
            } catch (Exception e) {
                emitters.remove(sessionId);
            }
        }, 20, 20, TimeUnit.SECONDS);
        return emitter;
    }

    public boolean send(String sessionId, String event, String data) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) return false;
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            return true;
        } catch (Exception e) {
            emitters.remove(sessionId);
            return false;
        }
    }
}