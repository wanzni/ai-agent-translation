package cn.net.susan.ai.translation.entity;

import cn.net.susan.ai.translation.enums.UserRoleEnum;
import cn.net.susan.ai.translation.enums.UserStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * 存储系统用户的基本信息，包括登录凭据、个人资料、偏好设置等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 邮箱
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 密码哈希
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * 昵称
     */
    @Column(name = "nickname", length = 100)
    private String nickname;

    /**
     * 头像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 语言偏好
     */
    @Column(name = "language_preference", length = 10)
    @Builder.Default
    private String languagePreference = "zh";

    /**
     * 时区
     */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Shanghai";

    /**
     * 用户状态
     */
    @Convert(converter = cn.net.susan.ai.translation.converter.UserStatusEnumConverter.class)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatusEnum status = UserStatusEnum.ACTIVE;

    /**
     * 用户角色
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRoleEnum role = UserRoleEnum.USER;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 更新最后登录信息
     * 
     * @param ipAddress IP地址
     */
    public void updateLastLogin(String ipAddress) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
    }

    /**
     * 检查是否为管理员
     * 
     * @return 是否为管理员
     */
    public boolean isAdmin() {
        return this.role == UserRoleEnum.ADMIN || this.role == UserRoleEnum.SUPER_ADMIN;
    }

    /**
     * 检查是否为超级管理员
     * 
     * @return 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return this.role == UserRoleEnum.SUPER_ADMIN;
    }

    /**
     * 检查用户是否活跃
     * 
     * @return 是否活跃
     */
    public boolean isActive() {
        return this.status == UserStatusEnum.ACTIVE;
    }
}

