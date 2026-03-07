package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.dto.CreateOrderRequest;
import cn.net.susan.ai.translation.dto.CreateOrderResponse;
import cn.net.susan.ai.translation.dto.PayRequest;
import cn.net.susan.ai.translation.dto.PaymentResponse;
import cn.net.susan.ai.translation.dto.PaymentPrepayRequest;
import cn.net.susan.ai.translation.dto.PaymentPrepayResponse;
import cn.net.susan.ai.translation.dto.PaymentConfirmRequest;
import cn.net.susan.ai.translation.dto.OrderDetailResponse;
import cn.net.susan.ai.translation.dto.OrdersListResponse;
import cn.net.susan.ai.translation.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import cn.net.susan.ai.translation.security.UserContext;

/**
 * 订单与支付控制器，提供会员订单创建、查询以及支付相关功能。
 * <p>
 * 该控制器遵循单一职责原则，仅负责路由和参数绑定，所有业务逻辑均委托给 {@link OrderPaymentService}。
 * 异常处理和响应格式化由全局处理器统一管理。
 */
@RestController
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;

    /**
     * 创建会员订单。
     *
     * @param req 包含订单信息的请求体
     * @return {@link CreateOrderResponse} 包含订单号和状态的响应
     */
    @PostMapping("/api/membership/order")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest req) {
        return orderPaymentService.createOrder(req);
    }

    /**
     * 根据订单号查询订单详情。
     *
     * @param orderNo 订单号
     * @return {@link OrderDetailResponse} 订单详细信息
     */
    @GetMapping("/api/membership/order/{orderNo}")
    public OrderDetailResponse getOrder(@PathVariable String orderNo) {
        return orderPaymentService.getOrder(orderNo);
    }

    /**
     * 查询当前认证用户的订单列表。
     *
     * @return {@link OrdersListResponse} 包含订单列表的响应
     */
    @GetMapping("/api/membership/orders")
    public OrdersListResponse listOrders() {
        Long userId = UserContext.getUserId();
        return orderPaymentService.listOrders(userId);
    }

    /**
     * 支付订单（具体支付方式由请求参数决定）。
     *
     * @param req 包含订单号和支付方式的请求
     * @return {@link PaymentResponse} 支付结果响应
     */
    @PostMapping("/api/payment/pay")
    public PaymentResponse pay(@RequestBody PayRequest req) {
        return orderPaymentService.pay(req);
    }

    /**
     * 扫码支付预下单，生成支付二维码。
     *
     * @param req 包含订单号和支付方式的预下单请求
     * @return {@link PaymentPrepayResponse} 包含二维码信息的响应
     */
    @PostMapping("/api/payment/prepay")
    public PaymentPrepayResponse prepay(@RequestBody PaymentPrepayRequest req) {
        return orderPaymentService.prepay(req);
    }

    /**
     * 确认支付状态（通常用于模拟支付回调或主动查询）。
     *
     * @param req 包含订单号的确认请求
     * @return {@link PaymentResponse} 支付确认结果
     */
    @PostMapping("/api/payment/confirm")
    public PaymentResponse confirm(@RequestBody PaymentConfirmRequest req) {
        return orderPaymentService.confirm(req);
    }
    
}