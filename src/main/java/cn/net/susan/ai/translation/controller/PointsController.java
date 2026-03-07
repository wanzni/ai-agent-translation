package cn.net.susan.ai.translation.controller;

import cn.net.susan.ai.translation.service.PointsService;
import cn.net.susan.ai.translation.dto.PointsBalanceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import cn.net.susan.ai.translation.security.UserContext;
import org.springframework.web.bind.annotation.RestController;


/**
 * 点数相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Validated
public class PointsController {

    private final PointsService pointsService;

    /**
     * 查询当前认证用户的点数余额。
     *
     * @return {@link PointsBalanceResponse} 包含用户点数余额的响应
     */
    @GetMapping("/balance")
    public PointsBalanceResponse getBalance() {
        Long userId = UserContext.getUserId();
        long balance = pointsService.getBalance(userId);
        return PointsBalanceResponse.builder().balance(balance).build();
    }
}