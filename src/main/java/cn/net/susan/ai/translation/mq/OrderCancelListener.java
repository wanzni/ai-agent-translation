package cn.net.susan.ai.translation.mq;

import cn.net.susan.ai.translation.entity.MembershipOrder;
import cn.net.susan.ai.translation.enums.OrderStatusEnum;
import cn.net.susan.ai.translation.repository.MembershipOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(topic = OrderCancelProducer.TOPIC, consumerGroup = "membership-order-cancel-group")
public class OrderCancelListener implements RocketMQListener<OrderCancelMessage> {

    private final MembershipOrderRepository orderRepository;

    @Override
    public void onMessage(OrderCancelMessage msg) {
        try {
            String orderNo = msg.getOrderNo();
            Optional<MembershipOrder> opt = orderRepository.findByOrderNo(orderNo);
            if (opt.isEmpty()) {
                log.warn("[MQ] cancel skip: order not found, orderNo={}", orderNo);
                return;
            }
            MembershipOrder order = opt.get();
            if (order.getStatus() != OrderStatusEnum.PENDING) {
                log.info("[MQ] cancel skip: status={}, orderNo={}", order.getStatus(), orderNo);
                return;
            }
            // 保障只在超时后取消（避免提前消息或时钟差异）
            LocalDateTime expireAt = msg.getExpireAt();
            if (expireAt != null && LocalDateTime.now().isBefore(expireAt)) {
                log.info("[MQ] cancel skip: not expired yet, orderNo={}, expireAt={} now={}", orderNo, expireAt, LocalDateTime.now());
                return;
            }
            order.setStatus(OrderStatusEnum.CANCELLED);
            orderRepository.save(order);
            log.info("[MQ] order cancelled due to timeout: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("[MQ] error while cancelling order: {}", e.getMessage(), e);
            // RocketMQ 默认会进行重试，这里不手动抛错以避免堆积，可视具体容错策略调整
        }
    }
}