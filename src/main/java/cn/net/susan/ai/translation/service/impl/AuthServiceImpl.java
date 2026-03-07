package cn.net.susan.ai.translation.service.impl;

import cn.net.susan.ai.translation.dto.auth.AuthUserResponse;
import cn.net.susan.ai.translation.dto.auth.LoginResponse;
import cn.net.susan.ai.translation.entity.User;
import cn.net.susan.ai.translation.enums.UserRoleEnum;
import cn.net.susan.ai.translation.enums.UserStatusEnum;
import cn.net.susan.ai.translation.repository.UserRepository;
import cn.net.susan.ai.translation.service.AuthService;
import cn.net.susan.ai.translation.util.PasswordUtils;
import cn.net.susan.ai.translation.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户认证服务实现类，提供用户注册和登录功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 通过手机号注册新用户。
     *
     * @param phone 手机号
     * @param password 密码
     * @param nickname 昵称
     * @return 注册成功的用户信息
     * @throws Exception 注册失败时抛出异常
     */
    @Override
    public AuthUserResponse registerByPhone(String phone, String password, String nickname) throws Exception {
        // 统一去除空白
        phone = phone == null ? null : phone.trim();
        nickname = nickname == null ? null : nickname.trim();

        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码长度需在6-64位之间");
        }

        // 检查手机号是否已注册
        userRepository.findByPhone(phone).ifPresent(u -> {
            throw new IllegalArgumentException("该手机号已注册");
        });

        // 生成用户名/邮箱以满足现有非空约束
        String username = phone;
        String email = phone.replaceAll("[^0-9]", "") + "@local.invalid";
        // 唯一性兜底
        if (userRepository.findByUsername(username).isPresent()) {
            username = username + "_" + System.currentTimeMillis();
        }
        if (userRepository.findByEmail(email).isPresent()) {
            email = username + "@local.invalid";
        }

        // 哈希密码（含随机盐）
        String salt = PasswordUtils.generateSalt();
        String passwordHash = PasswordUtils.hashPassword(password, salt);

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .nickname(nickname)
                .phone(phone)
                .status(UserStatusEnum.ACTIVE)
                .role(UserRoleEnum.USER)
                .build();

        user = userRepository.save(user);
        log.info("手机号注册成功: userId={}, phone={}", user.getId(), phone);

        return toAuthUserResponse(user);
    }

    public static void main(String[] args) {
        String password = "Li123456";
        String salt = PasswordUtils.generateSalt();
        String passwordHash = PasswordUtils.hashPassword(password, salt);
        System.out.println(passwordHash);
    }

    /**
     * 通过手机号和密码登录。
     *
     * @param phone 手机号
     * @param password 密码
     * @param ipAddress 登录IP地址
     * @return 登录成功后的响应，包含用户信息和token
     * @throws Exception 登录失败时抛出异常
     */
    @Override
    public LoginResponse loginByPhone(String phone, String password, String ipAddress) throws Exception {
        phone = phone == null ? null : phone.trim();
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        var userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在或手机号未注册");
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            throw new IllegalStateException("用户状态异常，无法登录");
        }
        if (!PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("手机号或密码错误");
        }

        user.updateLastLogin(ipAddress);
        userRepository.save(user);

        long expiresInSeconds = 7 * 24 * 3600L; // 7天有效期
        var claims = new HashMap<String, Object>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtil.generateToken(String.valueOf(user.getId()), expiresInSeconds * 1000, claims);

        // 保存登录态到Redis，key为token，值为用户JSON
        String redisKey = "auth:token:" + token;
        String userJson = objectMapper.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set(redisKey, userJson, expiresInSeconds, TimeUnit.SECONDS);

        return LoginResponse.builder()
                .user(toAuthUserResponse(user))
                .token(token)
                .expiresIn(expiresInSeconds)
                .build();
    }

    /**
     * 通过手机号和验证码登录（不需要密码）。
     *
     * @param phone 手机号
     * @param ipAddress 登录IP地址
     * @return 登录成功后的响应，包含用户信息和token
     * @throws Exception 登录失败时抛出异常
     */
    @Override
    public LoginResponse loginByPhoneWithCodeOnly(String phone, String ipAddress) throws Exception {
        phone = phone == null ? null : phone.trim();
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        var userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户不存在或手机号未注册");
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            throw new IllegalStateException("用户状态异常，无法登录");
        }

        user.updateLastLogin(ipAddress);
        userRepository.save(user);

        long expiresInSeconds = 7 * 24 * 3600L; // 7天有效期
        var claims = new HashMap<String, Object>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtil.generateToken(String.valueOf(user.getId()), expiresInSeconds * 1000, claims);

        // 保存登录态到Redis，key为token，值为用户JSON
        String redisKey = "auth:token:" + token;
        String userJson = objectMapper.writeValueAsString(user);
        stringRedisTemplate.opsForValue().set(redisKey, userJson, expiresInSeconds, TimeUnit.SECONDS);

        return LoginResponse.builder()
                .user(toAuthUserResponse(user))
                .token(token)
                .expiresIn(expiresInSeconds)
                .build();
    }

    private AuthUserResponse toAuthUserResponse(User u) {
        return AuthUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .phone(u.getPhone())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole().name())
                .status(u.getStatus().name())
                .build();
    }
}