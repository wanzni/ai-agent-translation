package cn.net.susan.ai.translation.service.impl;

import cn.net.susan.ai.translation.dto.*;
import cn.net.susan.ai.translation.entity.MembershipOrder;
import cn.net.susan.ai.translation.entity.PaymentRecord;
import cn.net.susan.ai.translation.entity.UserMembership;
import cn.net.susan.ai.translation.enums.MembershipTypeEnum;
import cn.net.susan.ai.translation.enums.OrderStatusEnum;
import cn.net.susan.ai.translation.enums.MembershipStatusEnum;
import cn.net.susan.ai.translation.mq.OrderCancelProducer;
import cn.net.susan.ai.translation.repository.MembershipOrderRepository;
import cn.net.susan.ai.translation.repository.PaymentRecordRepository;
import cn.net.susan.ai.translation.repository.UserRepository;
import cn.net.susan.ai.translation.service.MembershipService;
import cn.net.susan.ai.translation.service.PointsService;
import cn.net.susan.ai.translation.service.OrderPaymentService;
import cn.net.susan.ai.translation.enums.PaymentStatusEnum;
import cn.net.susan.ai.translation.payment.PaymentChannelRegistry;
import cn.net.susan.ai.translation.payment.PaymentQrPrepayResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;


/**
 * 订单与支付服务实现类，负责订单创建、查询、支付与会员开通。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private static final Map<MembershipTypeEnum, BigDecimal> PRICES = Map.of(
            MembershipTypeEnum.MONTHLY, new BigDecimal("29.00"),
            MembershipTypeEnum.QUARTERLY, new BigDecimal("79.00"),
            MembershipTypeEnum.YEARLY, new BigDecimal("299.00")
    );

    private final MembershipOrderRepository orderRepository;
    private final PaymentRecordRepository paymentRepository;
    private final MembershipService membershipService;
    private final PointsService pointsService;
    private final OrderCancelProducer orderCancelProducer;
    private final UserRepository userRepository;
    private final PaymentChannelRegistry channelRegistry;
    @Value("${app.membership.monthly-quota:5000}")
    private long monthlyQuotaCfg;
    @Value("${app.membership.subscribe-bonus-points:5000}")
    private long subscribeBonusPointsCfg;


    /**
     * 创建会员订单（生成订单号、计算金额与配额、安排延时取消）
     * @param req 创建订单请求，包含用户ID与会员类型等
     * @return 订单创建结果
     */
    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest req) {
        MembershipTypeEnum type = req.getType();
        int months = switch (type) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case YEARLY -> 12;
        };
        BigDecimal amount = PRICES.get(type);
        String orderNo = "MO" + System.currentTimeMillis();

        MembershipOrder order = MembershipOrder.builder()
                .orderNo(orderNo)
                .userId(req.getUserId())
                .membershipType(type)
                .months(months)
                .periodQuota(monthlyQuotaCfg * months)
                .amount(amount)
                .status(OrderStatusEnum.PENDING)
                .description("会员订单：" + type)
                .build();
        orderRepository.save(order);
        try {
            LocalDateTime expireAt = LocalDateTime.now().plusMinutes(30);
            orderCancelProducer.sendCancel(order.getOrderNo(), expireAt);
        } catch (Exception ignored) {}

        log.info("创建会员订单成功: orderNo={}, userId={}, 类型={}, 金额={}", orderNo, req.getUserId(), type, amount);

        return new CreateOrderResponse(
                order.getOrderNo(),
                order.getUserId(),
                order.getMembershipType().name(),
                order.getMonths(),
                order.getAmount(),
                order.getCurrency(),
                order.getPeriodQuota(),
                order.getCreatedAt(),
                (order.getCreatedAt() == null ? LocalDateTime.now().plusMinutes(30) : order.getCreatedAt().plusMinutes(30))
        );
    }

    /**
     * 查询订单详情（包含会员信息、支付状态、时间区间等）
     * @param orderNo 订单号
     * @return 订单详情DTO
     */
    @Override
    public OrderDetailResponse getOrder(String orderNo) {
        MembershipOrder o = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        Long uid = o.getUserId();
        String accountId = uid != null ? String.valueOf(uid) : null;
        String accountName = null;
        if (uid != null) {
            accountName = userRepository.findById(uid)
                    .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                    .orElse(null);
        }
        return new OrderDetailResponse(
                o.getOrderNo(),
                o.getUserId(),
                accountId,
                accountName,
                o.getMembershipType().name(),
                o.getMonths(),
                o.getAmount(),
                o.getCurrency(),
                o.getStatus().name(),
                o.getPeriodQuota(),
                o.getPaidAt(),
                o.getStartAt(),
                o.getEndAt(),
                o.getCreatedAt(),
                (o.getCreatedAt() == null ? null : o.getCreatedAt().plusMinutes(30))
        );
    }


    /**
     * 按用户查询订单列表（按创建时间倒序）
     * @param userId 用户ID
     * @return 订单列表DTO
     */
    @Override
    public OrdersListResponse listOrders(Long userId) {
        List<MembershipOrder> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderListItem> data = new ArrayList<>();
        for (MembershipOrder o : orders) {
            data.add(new OrderListItem(
                    o.getOrderNo(),
                    String.valueOf(o.getUserId()),
                    o.getMembershipType().name(),
                    o.getMonths(),
                    o.getAmount(),
                    o.getCurrency(),
                    o.getStatus().name(),
                    o.getPaidAt(),
                    o.getCreatedAt(),
                    (o.getCreatedAt() == null ? null : o.getCreatedAt().plusMinutes(30))
            ));
        }
        return new OrdersListResponse(data);
    }

    /**
     * 支付订单（写入支付记录、更新订单状态、开通会员资格）
     * @param req 支付请求，包含订单号、支付方式、支付渠道等
     * @return 支付结果DTO
     */
    @Override
    @Transactional
    public PaymentResponse pay(PayRequest req) {
        log.info("收到直接支付请求: orderNo={}, provider={}, method={}", req.getOrderNo(), req.getProvider(), req.getMethod());
        MembershipOrder order = orderRepository.findByOrderNo(req.getOrderNo())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        ensureOrderPayableOrThrow(order);

        String paymentNo = generatePaymentNo();
        PaymentRecord record = createSuccessRecordForPay(order, req.getProvider(), req.getMethod(), paymentNo);
        paymentRepository.save(record);

        log.info("直接支付成功（模拟）: orderNo={}, paymentNo={}, transactionNo={}", order.getOrderNo(), record.getPaymentNo(), record.getTransactionNo());

        return finalizeSuccessPayment(order, record);
    }

    /**
     * 预下单（二维码支付场景）
     *
     * - 校验订单可支付并创建支付记录（INITIATED）；
     * - 返回二维码链接与过期时间，供前端生成二维码展示；
     * - 支持 ALIPAY_QR/WECHAT_QR 等扫码方式。
     */
    @Override
    @Transactional
    public PaymentPrepayResponse prepay(PaymentPrepayRequest req) {
        log.info("收到扫码预下单请求: orderNo={}, provider={}, method={}", req.getOrderNo(), req.getProvider(), req.getMethod());
        MembershipOrder order = orderRepository.findByOrderNo(req.getOrderNo())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        ensureOrderPayableOrThrow(order);

        String paymentNo = generatePaymentNo();
        PaymentRecord record = createInitiatedRecord(order, req.getProvider(), req.getMethod(), paymentNo);
        paymentRepository.save(record);

        // 委派到具体渠道进行预下单
        PaymentQrPrepayResult prepayResult = channelRegistry.get(req.getProvider())
                .prepayQr(order, req.getMethod(), paymentNo);

        log.info("渠道预下单成功: provider={}, method={}, paymentNo={}, codeUrl={}", req.getProvider(), req.getMethod(), paymentNo, prepayResult.getCodeUrl());

        // 二维码有效期（与订单剩余时间取最小值）
        LocalDateTime expireAt = computeExpireAt(order);
        LocalDateTime qrExpire = prepayResult.getExpiresAt();
        if (expireAt != null && qrExpire != null && expireAt.isBefore(qrExpire)) {
            qrExpire = expireAt;
        }

        return new PaymentPrepayResponse(order.getOrderNo(), paymentNo, prepayResult.getCodeUrl(), qrExpire,
                req.getProvider().name(), req.getMethod().name());
    }

    /**
     * 确认支付（模拟回调）
     */
    @Override
    @Transactional
    public PaymentResponse confirm(PaymentConfirmRequest req) {
        log.info("收到确认支付请求: paymentNo={}", req.getPaymentNo());
        PaymentRecord record = paymentRepository.findTopByPaymentNo(req.getPaymentNo())
                .orElseThrow(() -> new IllegalArgumentException("支付记录不存在"));
        MembershipOrder order = orderRepository.findById(record.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        ensureOrderPayableOrThrow(order);

        record.setStatus(PaymentStatusEnum.SUCCESS);
        record.setTransactionNo("TXN-" + java.util.UUID.randomUUID());
        record.setPaidAt(LocalDateTime.now());
        paymentRepository.save(record);

        log.info("确认支付成功（模拟）: orderNo={}, paymentNo={}, transactionNo={}", order.getOrderNo(), record.getPaymentNo(), record.getTransactionNo());

        return finalizeSuccessPayment(order, record);
    }

    /**
     * 计算订单过期时间（创建时间 + 30 分钟）。
     *
     * @param order 订单
     * @return 过期时间
     */
    private LocalDateTime computeExpireAt(MembershipOrder order) {
        return order.getCreatedAt() != null ? order.getCreatedAt().plusMinutes(30) : null;
    }

    /**
     * 校验订单为可支付状态且未过期，否则抛出异常并必要时置为取消。
     *
     * @param order 订单
     */
    private void ensureOrderPayableOrThrow(MembershipOrder order) {
        if (order.getStatus() != OrderStatusEnum.PENDING) {
            throw new IllegalStateException("订单不是待支付状态");
        }
        LocalDateTime expireAt = computeExpireAt(order);
        if (expireAt != null && LocalDateTime.now().isAfter(expireAt)) {
            order.setStatus(OrderStatusEnum.CANCELLED);
            orderRepository.save(order);
            throw new IllegalStateException("订单已过期");
        }
    }

    /**
     * 生成支付单号。
     *
     * @return 支付单号
     */
    private String generatePaymentNo() {
        return "PR" + System.currentTimeMillis();
    }

    /**
     * 创建预下单支付记录（INITIATED）。
     *
     * @param order 订单
     * @param provider 支付服务商
     * @param method 支付方式
     * @param paymentNo 支付单号
     * @return 支付记录
     */
    private PaymentRecord createInitiatedRecord(MembershipOrder order, cn.net.susan.ai.translation.enums.PaymentProviderEnum provider,
                                               cn.net.susan.ai.translation.enums.PaymentMethodEnum method, String paymentNo) {
        return PaymentRecord.builder()
                .orderId(order.getId())
                .paymentNo(paymentNo)
                .provider(provider)
                .method(method)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(PaymentStatusEnum.INITIATED)
                .build();
    }

    /**
     * 创建直接支付成功记录（SUCCESS）。
     *
     * @param order 订单
     * @param provider 支付服务商
     * @param method 支付方式
     * @param paymentNo 支付单号
     * @return 支付记录
     */
    private PaymentRecord createSuccessRecordForPay(MembershipOrder order, cn.net.susan.ai.translation.enums.PaymentProviderEnum provider,
                                                    cn.net.susan.ai.translation.enums.PaymentMethodEnum method, String paymentNo) {
        return PaymentRecord.builder()
                .orderId(order.getId())
                .paymentNo(paymentNo)
                .provider(provider)
                .method(method)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(PaymentStatusEnum.SUCCESS)
                .transactionNo("TXN-" + java.util.UUID.randomUUID())
                .paidAt(LocalDateTime.now())
                .build();
    }

    /**
     * 支付成功后的收尾工作，包括更新订单状态、开通会员、赠送积分等。
     *
     * @param order 订单
     * @param record 支付记录
     * @return 支付响应
     */
    private PaymentResponse finalizeSuccessPayment(MembershipOrder order, PaymentRecord record) {
        boolean firstPayment = (order.getPaymentCount() == 0);
        order.setStatus(OrderStatusEnum.PAID);
        order.setPaidAt(record.getPaidAt());
        order.setPaymentCount(order.getPaymentCount() + 1);
        order.setLatestPaymentId(record.getId());
        order.setStartAt(LocalDateTime.now());
        order.setEndAt(LocalDateTime.now().plusMonths(order.getMonths()));
        orderRepository.save(order);

        long quota = monthlyQuotaCfg * order.getMonths();
        var membership = membershipService.subscribe(order.getUserId(), order.getMembershipType(), quota, order.getStartAt(), order.getEndAt());

        // 会员开通后赠送点数（仅首笔成功支付时触发，避免重复赠送）
        if (firstPayment) {
            try {
                long bonus = Math.max(0L, subscribeBonusPointsCfg);
                if (bonus > 0) {
                    pointsService.add(order.getUserId(), bonus, "会员开通赠送点数", "membership:" + membership.getId());
                    log.info("会员赠送点数成功: orderNo={}, userId={}, added={}", order.getOrderNo(), order.getUserId(), bonus);
                }
            } catch (Exception e) {
                log.warn("会员赠送点数失败，但会员已开通: orderNo={}, userId={}, err={}", order.getOrderNo(), order.getUserId(), e.getMessage());
            }
        }

        log.info("订单支付完成，会员已开通: orderNo={}, userId={}, 类型={}, 月数={}", order.getOrderNo(), order.getUserId(), order.getMembershipType(), order.getMonths());

        return new PaymentResponse(
                record.getPaymentNo(),
                record.getTransactionNo(),
                order.getOrderNo(),
                record.getPaidAt(),
                record.getAmount(),
                record.getCurrency(),
                record.getProvider().name(),
                record.getStatus().name(),
                order.getUserId(),
                order.getMembershipType().name(),
                order.getMonths(),
                order.getPeriodQuota(),
                order.getStartAt(),
                order.getEndAt()
        );
    }
}