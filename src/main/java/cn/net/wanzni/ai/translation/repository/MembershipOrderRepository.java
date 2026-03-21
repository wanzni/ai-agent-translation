package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.MembershipOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会员订单仓库
 *
 * 提供会员订单的基础查询能力：
 * - 通过订单号查询唯一订单；
 * - 按创建时间倒序查询用户的所有订单列表。
 */
@Repository
public interface MembershipOrderRepository extends JpaRepository<MembershipOrder, Long> {
    /** 根据订单号查询 */
    Optional<MembershipOrder> findByOrderNo(String orderNo);

    /** 查询用户的订单列表（按创建时间倒序） */
    List<MembershipOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}