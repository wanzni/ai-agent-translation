package cn.net.wanzni.ai.translation.service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
public class SseStreamService {

    /**
     * 向客户端发送 SSE 事件（仅 emitter，不经 Hub）。
     */
    public void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            log.warn("SSE 事件发送失败[name={}]: {}", name, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步发送到 emitter 与 SessionSseHub（Hub可为 null）。
     */
    public void sendEventAndHub(SseEmitter emitter, SessionSseHub hub, String sessionId, String name, String data) {
        sendEvent(emitter, name, data);
        if (hub != null && sessionId != null) {
            try {
                hub.send(sessionId, name, data);
            } catch (RuntimeException e) {
                log.warn("SSE Hub 事件发送失败[sessionId={}, name={}]: {}", sessionId, name, e.getMessage());
            }
        }
    }

    /**
     * 以固定分片与延迟流式发送文本，使用统一事件名。
     */
    public void streamChunks(SseEmitter emitter,
                             SessionSseHub hub,
                             String sessionId,
                             String text,
                             int chunkSize,
                             long delayMs,
                             String eventName) {
        if (text == null) text = "";
        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(text.length(), i + chunkSize));
            sendEventAndHub(emitter, hub, sessionId, eventName, chunk);
            sleepQuietly(delayMs);
        }
    }

    /**
     * 以固定分片与延迟流式发送文本，首帧使用 startEventName，后续使用 deltaEventName。
     */
    public void streamStartDelta(SseEmitter emitter,
                                 String text,
                                 int chunkSize,
                                 long delayMs,
                                 String startEventName,
                                 String deltaEventName) {
        if (text == null) text = "";
        boolean first = true;
        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(text.length(), i + chunkSize));
            String event = first ? startEventName : deltaEventName;
            sendEvent(emitter, event, chunk);
            first = false;
            sleepQuietly(delayMs);
        }
    }

    private void sleepQuietly(long ms) {
        try {
            if (ms > 0) Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}