package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 *
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum implements BaseEnum<Integer> {

    /**
     * 普通用户
     */
    USER(1, "普通用户"),

    /**
     * 管理员
     */
    ADMIN(2, "管理员"),

    /**
     * 超级管理员
     */
    SUPER_ADMIN(3, "超级管理员");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}