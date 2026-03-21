package cn.net.wanzni.ai.translation.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelMessage {
    private String orderNo;
    private LocalDateTime expireAt;
}