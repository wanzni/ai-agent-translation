package cn.net.wanzni.ai.translation.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
public class OrderCancelProducer {

    public static final String TOPIC = "membership-order-cancel";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送延迟取消消息，RocketMQ默认延迟级别16对应30分钟。
     */
    public void sendCancel(String orderNo, LocalDateTime expireAt) {
        try {
            OrderCancelMessage payload = new OrderCancelMessage(orderNo, expireAt);
            var message = MessageBuilder.withPayload(payload).build();
            int delayLevel30m = 16; // 30 minutes
            long timeoutMs = 3000L;
            rocketMQTemplate.syncSend(TOPIC, message, timeoutMs, delayLevel30m);
            log.info("[MQ] scheduled cancel for orderNo={} at {}", orderNo, expireAt);
        } catch (Exception e) {
            // 捕获异常，避免影响下单流程
            log.warn("[MQ] failed to schedule cancel for orderNo={}: {}", orderNo, e.getMessage());
        }
    }
}
