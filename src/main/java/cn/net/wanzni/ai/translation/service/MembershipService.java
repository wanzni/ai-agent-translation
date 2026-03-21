package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.entity.UserMembership;
import cn.net.wanzni.ai.translation.enums.MembershipTypeEnum;

/**
 * 会员服务接口，提供查询有效会员、订阅会员与配额消耗等能力。
 *
 * 控制器与业务层通过该接口协作，具体事务与异常由实现类与全局处理器保证一致性与可观测性。
 */
public interface MembershipService {
    /**
     * 查询用户当前有效会员。
     *
     * @param userId 用户ID
     * @return 用户会员信息
     */
    UserMembership getActiveMembership(Long userId);

    /**
     * 查询剩余配额。
     *
     * @param userId 用户ID
     * @return 剩余配额
     */
    long getRemainingQuota(Long userId);

    /**
     * 消耗配额。
     *
     * @param userId 用户ID
     * @param amount 消耗数量
     */
    void consumeQuota(Long userId, long amount);

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
    UserMembership subscribe(Long userId, MembershipTypeEnum type, long quota, java.time.LocalDateTime startAt, java.time.LocalDateTime endAt);
}