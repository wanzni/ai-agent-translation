package cn.net.susan.ai.translation.converter;

import cn.net.susan.ai.translation.enums.OrderStatusEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrderStatusEnumConverter extends EnumConverter<OrderStatusEnum> {
    public OrderStatusEnumConverter() {
        super(OrderStatusEnum.class);
    }
}