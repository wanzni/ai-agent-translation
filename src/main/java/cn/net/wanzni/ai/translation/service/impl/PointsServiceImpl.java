package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.entity.PointsAccount;
import cn.net.wanzni.ai.translation.entity.PointsTransaction;
import cn.net.wanzni.ai.translation.exception.InsufficientPointsException;
import cn.net.wanzni.ai.translation.repository.PointsAccountRepository;
import cn.net.wanzni.ai.translation.repository.PointsTransactionRepository;
import cn.net.wanzni.ai.translation.service.PointsService;
import cn.net.wanzni.ai.translation.enums.TransactionTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户点数服务实现类，提供点数账户管理、查询、扣减和增加等功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private final PointsAccountRepository pointsAccountRepository;
    private final PointsTransactionRepository pointsTransactionRepository;

    /**
     * 确保用户点数账户存在，如果不存在则创建。
     *
     * @param userId 用户ID
     */
    @Override
    public void ensureAccount(Long userId) {
        if (userId == null) {
            return;
        }
        pointsAccountRepository.findByUserId(userId).orElseGet(() -> {
            PointsAccount acc = PointsAccount.builder()
                    .userId(userId)
                    .balance(0L)
                    .build();
            return pointsAccountRepository.save(acc);
        });
    }

    /**
     * 查询用户点数余额。
     *
     * @param userId 用户ID
     * @return 点数余额
     */
    @Override
    public long getBalance(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return pointsAccountRepository.findByUserId(userId)
                .map(PointsAccount::getBalance)
                .orElse(0L);
    }

    /**
     * 扣减点数，如果点数不足则抛出异常。
     *
     * @param userId 用户ID
     * @param points 扣减的点数
     * @param reason 扣减原因
     * @param referenceId 关联业务ID
     * @return 实际扣减的数量
     */
    @Override
    public long deduct(Long userId, long points, String reason, String referenceId) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户ID，无法扣减点数");
        }
        if (points <= 0) {
            return 0L;
        }
        log.info("尝试扣减点数: userId={}, requiredPoints={}, referenceId={}, reason={} ", userId, points, referenceId, reason);
        PointsAccount acc = pointsAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new InsufficientPointsException("点数账户不存在或余额不足"));

        if (acc.getBalance() == null || acc.getBalance() < points) {
            long current = acc.getBalance() == null ? 0L : acc.getBalance();
            log.warn("点数余额不足: userId={}, need={}, balance={}, referenceId={}", userId, points, current, referenceId);
            throw new InsufficientPointsException("点数余额不足，所需: " + points + ", 当前: " + (acc.getBalance() == null ? 0 : acc.getBalance()));
        }

        acc.setBalance(acc.getBalance() - points);
        pointsAccountRepository.save(acc);

        PointsTransaction tx = PointsTransaction.builder()
                .userId(userId)
                .type(TransactionTypeEnum.DEDUCT)
                .delta(-points)
                .referenceId(referenceId)
                .reason(reason != null ? reason : "扣减点数")
                .createdAt(LocalDateTime.now())
                .build();
        pointsTransactionRepository.save(tx);

        log.info("扣减点数成功: userId={}, points={}, balance={}, referenceId={}", userId, points, acc.getBalance(), referenceId);
        return points;
    }

    /**
     * 增加点数。
     *
     * @param userId 用户ID
     * @param points 增加的点数
     * @param reason 增加原因
     * @param referenceId 关联业务ID
     */
    @Override
    public void add(Long userId, long points, String reason, String referenceId) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户ID，无法增加点数");
        }
        if (points <= 0) {
            return;
        }
        PointsAccount acc = pointsAccountRepository.findByUserId(userId)
                .orElseGet(() -> pointsAccountRepository.save(PointsAccount.builder()
                        .userId(userId)
                        .balance(0L)
                        .build()));

        acc.setBalance((acc.getBalance() == null ? 0L : acc.getBalance()) + points);
        pointsAccountRepository.save(acc);

        PointsTransaction tx = PointsTransaction.builder()
                .userId(userId)
                .type(TransactionTypeEnum.ADD)
                .delta(points)
                .referenceId(referenceId)
                .reason(reason != null ? reason : "增加点数")
                .createdAt(LocalDateTime.now())
                .build();
        pointsTransactionRepository.save(tx);

        log.info("增加点数成功: userId={}, points={}, balance={}", userId, points, acc.getBalance());
    }
}