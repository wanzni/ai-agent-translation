package cn.net.wanzni.ai.translation.service;

/**
 * 订单与支付服务接口
 *
 * 控制器只负责路由与参数绑定，真实业务逻辑在服务层实现。
 * 异常与统一返回由全局处理器负责（例如 RestControllerAdvice、ResponseBodyAdvice）。
 */

import cn.net.wanzni.ai.translation.dto.CreateOrderRequest;
import cn.net.wanzni.ai.translation.dto.CreateOrderResponse;
import cn.net.wanzni.ai.translation.dto.OrderDetailResponse;
import cn.net.wanzni.ai.translation.dto.OrdersListResponse;
import cn.net.wanzni.ai.translation.dto.PayRequest;
import cn.net.wanzni.ai.translation.dto.PaymentResponse;
import cn.net.wanzni.ai.translation.dto.PaymentPrepayRequest;
import cn.net.wanzni.ai.translation.dto.PaymentPrepayResponse;
import cn.net.wanzni.ai.translation.dto.PaymentConfirmRequest;

public interface OrderPaymentService {
    /**
     * 创建会员订单（生成订单号、计算金额与配额、安排延时取消）
     * @param req 创建订单请求，包含用户ID与会员类型等
     * @return 订单创建结果
     */
    CreateOrderResponse createOrder(CreateOrderRequest req);

    /**
     * 查询订单详情（包含会员信息、支付状态、时间区间等）
     * @param orderNo 订单号
     * @return 订单详情DTO
     */
    OrderDetailResponse getOrder(String orderNo);

    /**
     * 按用户查询订单列表（按创建时间倒序）
     * @param userId 用户ID
     * @return 订单列表DTO
     */
    OrdersListResponse listOrders(Long userId);

    /**
     * 支付订单（写入支付记录、更新订单状态、开通会员资格）
     * @param req 支付请求，包含订单号、支付方式、支付渠道等
     * @return 支付结果DTO
     */
    PaymentResponse pay(PayRequest req);

    /**
     * 预下单（二维码支付场景）
     *
     * - 校验订单可支付并创建支付记录（INITIATED）；
     * - 返回二维码链接与过期时间，供前端生成二维码展示；
     * - 支持 ALIPAY_QR/WECHAT_QR 等扫码方式。
     */
    PaymentPrepayResponse prepay(PaymentPrepayRequest req);

    /**
     * 确认支付完成（模拟三方回调）
     *
     * - 根据支付单号将支付记录置为 SUCCESS；
     * - 更新订单状态并开通会员；
     * - 返回支付结果用于跳转成功页。
     */
    PaymentResponse confirm(PaymentConfirmRequest req);
}