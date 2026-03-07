package cn.net.susan.ai.translation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会员订阅请求
 */
@Data
public class MembershipSubscribeRequest {
    @NotNull
    private Long userId;

    private int type;
}