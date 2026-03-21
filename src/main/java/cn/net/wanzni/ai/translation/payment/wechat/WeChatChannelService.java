package cn.net.wanzni.ai.translation.payment.wechat;

import cn.net.wanzni.ai.translation.entity.MembershipOrder;
import cn.net.wanzni.ai.translation.enums.PaymentMethodEnum;
import cn.net.wanzni.ai.translation.enums.PaymentProviderEnum;
import cn.net.wanzni.ai.translation.payment.PaymentChannelService;
import cn.net.wanzni.ai.translation.payment.PaymentQrPrepayResult;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import cn.net.wanzni.ai.translation.config.PayProviderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 微信支付渠道实现：
 * - 采用构造注入统一管理依赖；
 * - NativePayService 作为可选依赖，通过 ObjectProvider 获取；
 * - 未配置或异常时降级到本地 Mock 页面。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeChatChannelService implements PaymentChannelService {
    private final ObjectProvider<NativePayService> nativePayServiceProvider; // 可选依赖
    private final PayProviderProperties.WeChat wechatProps;

    @Value("${app.base-url:http://localhost}")
    private String appBaseUrl;
    @Override
    public PaymentProviderEnum provider() { return PaymentProviderEnum.WECHAT; }

    @Override
    public PaymentQrPrepayResult prepayQr(MembershipOrder order, PaymentMethodEnum method, String paymentNo) {
        try {
            NativePayService nativePayService = nativePayServiceProvider.getIfAvailable();
            if (nativePayService == null) throw new IllegalStateException("未配置微信支付");

            PrepayRequest req = new PrepayRequest();
            Amount amount = new Amount();
            amount.setTotal(order.getAmount().multiply(java.math.BigDecimal.valueOf(100)).intValue());
            req.setAmount(amount);
            // appid 和 mchid 从配置中由Service层创建时注入，SDK内部会使用它们进行签名
            req.setAppid(wechatProps.getAppId());
            req.setMchid(wechatProps.getMchid());
            req.setDescription(order.getDescription() != null ? order.getDescription() : "Membership purchase");
            req.setNotifyUrl(wechatProps.getNotifyUrl());
            req.setOutTradeNo(paymentNo);

            PrepayResponse resp = nativePayService.prepay(req);
            if (resp == null || resp.getCodeUrl() == null || resp.getCodeUrl().isBlank()) {
                throw new IllegalStateException("微信预下单失败");
            }
            LocalDateTime expire = LocalDateTime.now().plusMinutes(15);
            return new PaymentQrPrepayResult(resp.getCodeUrl(), expire);
        } catch (Exception e) {
            // 降级：跳转本地模拟收银台页面
            log.warn("微信预下单降级，原因：{}", e.getMessage());
            String codeUrl = appBaseUrl + "/mock/wechat/unifiedorder?paymentNo=" + paymentNo;
            LocalDateTime expire = LocalDateTime.now().plusMinutes(10);
            return new PaymentQrPrepayResult(codeUrl, expire);
        }
    }
}