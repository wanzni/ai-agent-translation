package cn.net.susan.ai.translation.payment;

import cn.net.susan.ai.translation.enums.PaymentProviderEnum;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 支付渠道注册表
 *
 * 将所有实现了 PaymentChannelService 的渠道按 provider 枚举注册到 Map，
 * 供业务层按渠道快速获取对应实现。构造函数注入 List<PaymentChannelService>
 * 由Spring自动收集同接口的所有实现。
 */
@Component
public class PaymentChannelRegistry {
    private final Map<PaymentProviderEnum, PaymentChannelService> registry = new EnumMap<>(PaymentProviderEnum.class);

    /**
     * 构造注入所有渠道实现并建立枚举到实现的映射
     */
    public PaymentChannelRegistry(List<PaymentChannelService> channels) {
        for (PaymentChannelService c : channels) {
            registry.put(c.provider(), c);
        }
    }

    /**
     * 根据渠道枚举获取渠道实现
     * @param provider 渠道枚举
     * @return 渠道实现
     * @throws IllegalArgumentException 当渠道未注册时抛出
     */
    public PaymentChannelService get(PaymentProviderEnum provider) {
        PaymentChannelService svc = registry.get(provider);
        if (svc == null) throw new IllegalArgumentException("不支持的支付渠道：" + provider);
        return svc;
    }
}