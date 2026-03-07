package cn.net.susan.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员方案响应
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlanResponse {
    /**
     * 类型
     */
    private int type;

    /**
     * 名称
     */
    private String name;

    /**
     * 价格
     */
    private int price;

    /**
     * 每月配额
     */
    private long monthlyQuota;
}