package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单列表响应
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdersListResponse {
    /**
     * 订单列表
     */
    private List<OrderListItem> data;
}