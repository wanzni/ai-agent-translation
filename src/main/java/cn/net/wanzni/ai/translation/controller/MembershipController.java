package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.MembershipPlanResponse;
import cn.net.wanzni.ai.translation.dto.MembershipCurrentResponse;
import cn.net.wanzni.ai.translation.dto.MembershipSubscribeRequest;
import cn.net.wanzni.ai.translation.entity.UserMembership;
import cn.net.wanzni.ai.translation.enums.MembershipTypeEnum;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.MembershipService;
import cn.net.wanzni.ai.translation.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import cn.net.wanzni.ai.translation.config.MembershipProperties;

/**
 * 会员管理控制器，提供会员方案查询和开通订阅功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;
    private final PointsService pointsService;
    private final MembershipProperties membershipProperties;

    /**
     * 获取可用的会员方案列表。
     * <p>
     * 这些方案通常是静态配置的，但未来可以扩展为从数据库或配置服务动态加载。
     *
     * @return {@link List<MembershipPlanResponse>} 会员方案列表
     */
    @GetMapping("/plans")
    public List<MembershipPlanResponse> plans() {
        long monthlyQuota = membershipProperties.getMonthlyQuota();
        List<MembershipPlanResponse> plans = List.of(
                MembershipPlanResponse.builder().type(MembershipTypeEnum.MONTHLY.ordinal()).name("包月会员").price(49).monthlyQuota(monthlyQuota).build(),
                MembershipPlanResponse.builder().type(MembershipTypeEnum.QUARTERLY.ordinal()).name("包季会员").price(139).monthlyQuota(monthlyQuota).build(),
                MembershipPlanResponse.builder().type(MembershipTypeEnum.YEARLY.ordinal()).name("包年会员").price(299).monthlyQuota(monthlyQuota).build()
        );
        return plans;
    }

    /**
     * 处理用户开通或续订会员的请求。
     * <p>
     * 此端点接收会员订阅请求，计算会员有效期和配额，然后调用 {@link MembershipService} 完成订阅。
     * 成功开通会员后，还会根据配置赠送一定数量的积分。
     *
     * @return {@link UserMembership} 创建或更新后的会员信息
     */
    @GetMapping("/current")
    public MembershipCurrentResponse current() {
        Long userId = UserContext.getUserId();
        UserMembership membership = membershipService.getActiveMembership(userId);
        if (membership == null) {
            return MembershipCurrentResponse.builder()
                    .active(false)
                    .periodQuota(0L)
                    .periodUsed(0L)
                    .remainingQuota(0L)
                    .build();
        }
        return MembershipCurrentResponse.builder()
                .active(membership.isActiveNow())
                .type(membership.getType() == null ? null : membership.getType().name())
                .typeDesc(membership.getType() == null ? null : membership.getType().getDesc())
                .status(membership.getStatus() == null ? null : membership.getStatus().name())
                .startAt(membership.getStartAt())
                .endAt(membership.getEndAt())
                .periodQuota(membership.getPeriodQuota())
                .periodUsed(membership.getPeriodUsed())
                .remainingQuota(membership.remainingQuota())
                .build();
    }

    @PostMapping("/subscribe")
    public UserMembership subscribe(@Validated @RequestBody MembershipSubscribeRequest payload) {
        log.info("会员开通请求: userId={}, type={}", payload.getUserId(), payload.getType());
        MembershipTypeEnum type = MembershipTypeEnum.values()[payload.getType()];

        // 计算会员周期与配额
        int months;
        switch (type) {
            case MONTHLY -> months = 1;
            case QUARTERLY -> months = 3;
            case YEARLY -> months = 12;
            default -> months = 1;
        }
        long monthlyQuota = membershipProperties.getMonthlyQuota();
        long quota = monthlyQuota * months;
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.plusMonths(months);

        UserMembership membership = membershipService.subscribe(payload.getUserId(), type, quota, startAt, endAt);

        // 会员开通后，赠送固定点数至用户点数余额（可配置）
        try {
            long bonus = membershipProperties.getSubscribeBonusPoints();
            pointsService.add(payload.getUserId(), bonus, "会员开通赠送点数", "membership:" + membership.getId());
            log.info("会员开通赠送点数成功: userId={}, added={}, membershipId={}", payload.getUserId(), bonus, membership.getId());
        } catch (Exception e) {
            log.warn("会员赠送点数失败，但会员已开通: userId={}, err={}", payload.getUserId(), e.getMessage());
        }

        return membership;
    }
}
