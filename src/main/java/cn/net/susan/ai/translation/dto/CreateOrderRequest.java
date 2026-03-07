package cn.net.susan.ai.translation.dto;

import cn.net.susan.ai.translation.enums.MembershipTypeEnum;
import lombok.Data;

/**
 * 会员订单创建请求
 */
@Data
public class CreateOrderRequest {
    private Long userId;
    /** 会员类型 */
    private MembershipTypeEnum type;
}