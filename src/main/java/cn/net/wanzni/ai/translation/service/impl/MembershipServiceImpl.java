package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.entity.UserMembership;
import cn.net.wanzni.ai.translation.enums.MembershipStatusEnum;
import cn.net.wanzni.ai.translation.enums.MembershipTypeEnum;
import cn.net.wanzni.ai.translation.exception.InsufficientPointsException;
import cn.net.wanzni.ai.translation.repository.UserMembershipRepository;
import cn.net.wanzni.ai.translation.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 会员服务实现类，负责会员查询、订阅与配额消耗。
 */
public class MembershipServiceImpl implements MembershipService {

    private final UserMembershipRepository userMembershipRepository;


    /**
     * 查询用户当前有效会员。
     *
     * @param userId 用户ID
     * @return 用户会员信息
     */
    @Override
    public UserMembership getActiveMembership(Long userId) {
        if (userId == null) {
            log.warn("查询会员时用户ID为空，返回null");
            return null;
        }
        return userMembershipRepository.findTopByUserIdOrderByEndAtDesc(userId)
                .filter(m -> m.getStatus() == MembershipStatusEnum.ACTIVE
                        && m.getEndAt() != null && m.getEndAt().isAfter(LocalDateTime.now()))
                .orElse(null);
    }


    /**
     * 查询剩余配额。
     *
     * @param userId 用户ID
     * @return 剩余配额
     */
    @Override
    public long getRemainingQuota(Long userId) {
        UserMembership m = getActiveMembership(userId);
        long quota = m == null ? 0L : m.remainingQuota();
        log.info("查询会员剩余配额: userId={}, remainQuota={}, hasActiveMembership={}", userId, quota, m != null);
        return quota;
    }


    /**
     * 消耗配额。
     *
     * @param userId 用户ID
     * @param amount 消耗数量
     */
    @Override
    public void consumeQuota(Long userId, long amount) {
        if (amount <= 0) {
            return;
        }
        UserMembership m = getActiveMembership(userId);
        if (m == null) {
            log.warn("会员配额消耗失败: 无有效会员 | userId={}, amount={}", userId, amount);
            throw new InsufficientPointsException("当前无有效会员或会员已过期");
        }
        long remain = m.remainingQuota();
        if (remain < amount) {
            log.warn("会员配额不足: userId={}, need={}, remain={}, type={}, periodQuota={}, periodUsed={}, endAt={}, status={}",
                    userId, amount, remain, m.getType(), m.getPeriodQuota(), m.getPeriodUsed(), m.getEndAt(), m.getStatus());
            throw new InsufficientPointsException("会员周期配额不足，所需: " + amount + ", 剩余: " + remain);
        }
        m.setPeriodUsed((m.getPeriodUsed() == null ? 0L : m.getPeriodUsed()) + amount);
        userMembershipRepository.save(m);
        log.info("消耗会员配额: userId={}, amount={}, remainAfter={} ", userId, amount, m.remainingQuota());
    }


    /**
     * 订阅会员（创建会员记录）。
     *
     * @param userId 用户ID
     * @param type 会员类型
     * @param quota 配额
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 用户会员信息
     */
    @Override
    public UserMembership subscribe(Long userId, MembershipTypeEnum type, long quota, LocalDateTime startAt, LocalDateTime endAt) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户ID，无法订阅会员");
        }
        if (type == null || startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("会员订阅参数不合法");
        }
        UserMembership m = UserMembership.builder()
                .userId(userId)
                .type(type)
                .status(MembershipStatusEnum.ACTIVE)
                .startAt(startAt)
                .endAt(endAt)
                .periodQuota(Math.max(0L, quota))
                .periodUsed(0L)
                .build();
        return userMembershipRepository.save(m);
    }
}