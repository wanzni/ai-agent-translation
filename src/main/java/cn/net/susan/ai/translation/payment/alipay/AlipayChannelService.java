package cn.net.susan.ai.translation.payment.alipay;

import cn.net.susan.ai.translation.entity.MembershipOrder;
import cn.net.susan.ai.translation.enums.PaymentMethodEnum;
import cn.net.susan.ai.translation.enums.PaymentProviderEnum;
import cn.net.susan.ai.translation.payment.PaymentChannelService;
import cn.net.susan.ai.translation.payment.PaymentQrPrepayResult;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付宝扫码支付渠道实现：
 * - 正常情况返回支付宝二维码内容；
 * - 异常或未配置时降级到本地 Mock 收银台页面。
 * app.base-url 用于移动端扫码可访问的主机地址（含端口）。
 */
@Service
@Slf4j
public class AlipayChannelService implements PaymentChannelService {

    @Value("${app.base-url:http://localhost}")
    private String appBaseUrl;
    @Override
    public PaymentProviderEnum provider() { return PaymentProviderEnum.ALIPAY; }

    @Override
    public PaymentQrPrepayResult prepayQr(MembershipOrder order, PaymentMethodEnum method, String paymentNo) {
        try {
            String subject = "Membership " + order.getMembershipType().name();
            String amount = order.getAmount().toPlainString();
            AlipayTradePrecreateResponse resp = Factory.Payment.FaceToFace().preCreate(subject, paymentNo, amount);
            if (resp == null || resp.getQrCode() == null || resp.getQrCode().isBlank()) {
                throw new IllegalStateException("支付宝预创建失败");
            }
            LocalDateTime expire = LocalDateTime.now().plusMinutes(15);
            return new PaymentQrPrepayResult(resp.getQrCode(), expire);
        } catch (Exception e) {
            // 降级为本地可访问的Mock页面，避免扫码后空白
            log.warn("支付宝预创建降级，原因：{}", e.getMessage());
            String codeUrl = appBaseUrl + "/mock/alipay/precreate?paymentNo=" + paymentNo;
            LocalDateTime expire = LocalDateTime.now().plusMinutes(10);
            return new PaymentQrPrepayResult(codeUrl, expire);
        }
    }
}