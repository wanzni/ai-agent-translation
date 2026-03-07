package cn.net.susan.ai.translation.service;

/**
 * 用户点数服务接口，提供点数账户管理、查询、扣减和增加等功能。
 */
public interface PointsService {
    /**
     * 确保用户点数账户存在，如果不存在则创建。
     *
     * @param userId 用户ID
     */
    void ensureAccount(Long userId);

    /**
     * 查询用户点数余额。
     *
     * @param userId 用户ID
     * @return 点数余额
     */
    long getBalance(Long userId);

    /**
     * 扣减点数，如果点数不足则抛出异常。
     *
     * @param userId 用户ID
     * @param points 扣减的点数
     * @param reason 扣减原因
     * @param referenceId 关联业务ID
     * @return 实际扣减的数量
     */
    long deduct(Long userId, long points, String reason, String referenceId);

    /**
     * 增加点数。
     *
     * @param userId 用户ID
     * @param points 增加的点数
     * @param reason 增加原因
     * @param referenceId 关联业务ID
     */
    void add(Long userId, long points, String reason, String referenceId);
}