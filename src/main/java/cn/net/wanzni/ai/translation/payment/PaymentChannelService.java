package cn.net.wanzni.ai.translation.payment;

import cn.net.wanzni.ai.translation.entity.MembershipOrder;
import cn.net.wanzni.ai.translation.enums.PaymentMethodEnum;
import cn.net.wanzni.ai.translation.enums.PaymentProviderEnum;

/**
 * 支付渠道业务接口
 *
 * 设计原则：
 * - 仅负责与三方SDK交互（统一下单、查询、关闭等）；
 * - 不直接更新订单或会员数据（订单收尾由服务层完成）；
 * - 返回必要的数据给服务层（如二维码链接与过期时间等）。
 */
public interface PaymentChannelService {
    /** 渠道标识（ALIPAY/WECHAT/UNIONPAY 等） */
    PaymentProviderEnum provider();

    /**
     * 扫码预下单
     *
     * @param order     订单实体
     * @param method    支付方式（如 ALIPAY_QR、WECHAT_QR）
     * @param paymentNo 支付单号（系统生成，保证幂等与唯一）
     * @return 二维码内容与有效期（用于前端生成二维码）
     */
    PaymentQrPrepayResult prepayQr(MembershipOrder order, PaymentMethodEnum method, String paymentNo);
}