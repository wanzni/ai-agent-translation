package cn.net.susan.ai.translation.payment.unionpay;

import cn.net.susan.ai.translation.entity.MembershipOrder;
import cn.net.susan.ai.translation.enums.PaymentMethodEnum;
import cn.net.susan.ai.translation.enums.PaymentProviderEnum;
import cn.net.susan.ai.translation.payment.PaymentChannelService;
import cn.net.susan.ai.translation.payment.PaymentQrPrepayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 银联/云闪付扫码渠道实现（占位版）：
 * 当前使用自定义 scheme 作为二维码内容；如需接入正式接口，
 * 需替换为银联官方预下单API并返回二维码链接。
 */
@Service
@Slf4j
public class UnionPayChannelService implements PaymentChannelService {
    @Override
    public PaymentProviderEnum provider() { return PaymentProviderEnum.UNIONPAY; }

    @Override
    public PaymentQrPrepayResult prepayQr(MembershipOrder order, PaymentMethodEnum method, String paymentNo) {
        // 目前为占位逻辑：使用自定义scheme供前端生成二维码
        log.info("银联模拟预下单，支付单号={}", paymentNo);
        String codeUrl = "unionpay://qr?paymentNo=" + paymentNo;
        LocalDateTime expire = LocalDateTime.now().plusMinutes(10);
        return new PaymentQrPrepayResult(codeUrl, expire);
    }
}