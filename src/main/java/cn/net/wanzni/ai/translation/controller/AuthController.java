package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.auth.AuthUserResponse;
import cn.net.wanzni.ai.translation.dto.auth.LoginResponse;
import cn.net.wanzni.ai.translation.dto.auth.LogoutResponse;
import cn.net.wanzni.ai.translation.dto.auth.PhoneLoginRequest;
import cn.net.wanzni.ai.translation.dto.auth.PhoneLoginWithCodeRequest;
import cn.net.wanzni.ai.translation.dto.auth.SendCodeResponse;
import cn.net.wanzni.ai.translation.dto.auth.SendPhoneCodeRequest;
import cn.net.wanzni.ai.translation.dto.auth.PhoneRegisterRequest;
import cn.net.wanzni.ai.translation.dto.auth.PhoneRegisterWithCodeRequest;
import cn.net.wanzni.ai.translation.service.AuthService;
import cn.net.wanzni.ai.translation.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;

/**
 * 认证授权控制器
 *
 * <p>该控制器负责处理所有与用户认证和授权相关的 API 请求，
 * 包括用户注册、登录、登出以及验证码的发送和校验。
 *
 * <p>主要功能包括：
 * <ul>
 *     <li>手机号+密码注册</li>
 *     <li>手机号+验证码注册</li>
 *     <li>手机号+密码登录</li>
 *     <li>手机号+验证码登录</li>
 *     <li>发送登录/注册验证码</li>
 *     <li>用户登出</li>
 * </ul>
 *
 * @version 1.0.0
 * @since 2024-07-28
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final VerificationCodeService verificationCodeService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 通过手机号和密码进行用户注册
     *
     * @param payload 包含手机号、密码和昵称的注册请求体
     * @return 注册成功的用户信息
     * @throws Exception 注册过程中可能抛出的异常
     */
    @PostMapping("/register/phone")
    public AuthUserResponse registerByPhone(@Valid @RequestBody PhoneRegisterRequest payload) throws Exception {
        log.info("开始手机号注册: {}", payload.getPhone());
        return authService.registerByPhone(payload.getPhone(), payload.getPassword(), payload.getNickname());
    }

    /**
     * 通过手机号和密码进行用户登录
     *
     * @param payload 包含手机号和密码的登录请求体
     * @param request HTTP 请求对象，用于获取客户端 IP 地址
     * @return 登录成功的响应，包含认证令牌
     * @throws Exception 登录过程中可能抛出的异常
     */
    @PostMapping("/login/phone")
    public LoginResponse loginByPhone(@Valid @RequestBody PhoneLoginRequest payload, HttpServletRequest request) throws Exception {
        String ip = request.getRemoteAddr();
        log.info("手机号登录请求: phone={}, ip={}", payload.getPhone(), ip);
        return authService.loginByPhone(payload.getPhone(), payload.getPassword(), ip);
    }

    /**
     * 发送手机登录验证码
     *
     * @param payload 包含手机号的请求体
     * @return 发送结果，包括是否成功、冷却时间等信息
     */
    @PostMapping("/code/phone")
    public SendCodeResponse sendLoginCode(@Valid @RequestBody SendPhoneCodeRequest payload) {
        boolean ok = verificationCodeService.sendLoginCode(payload.getPhone());
        String msg = ok ? "验证码已发送" : "发送过于频繁，请稍后再试";
        return SendCodeResponse.builder()
                .sent(ok)
                .cooldownSeconds(verificationCodeService.getSendCooldownSeconds())
                .expiresInSeconds(verificationCodeService.getCodeExpireSeconds())
                .message(msg)
                .build();
    }

    /**
     * 通过手机号和验证码进行用户登录
     *
     * @param payload 包含手机号、密码和验证码的登录请求体
     * @param request HTTP 请求对象，用于获取客户端 IP 地址
     * @return 登录成功的响应，包含认证令牌
     * @throws Exception 登录过程中可能抛出的异常
     */
    @PostMapping("/login/phone-code")
    public LoginResponse loginByPhoneWithCode(@Valid @RequestBody PhoneLoginWithCodeRequest payload, HttpServletRequest request) throws Exception {
        String ip = request.getRemoteAddr();
        log.info("手机号+验证码登录请求: phone={}, ip={}", payload.getPhone(), ip);
        boolean verified = verificationCodeService.validateAndConsume(payload.getPhone(), payload.getCode());
        if (!verified) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        // 验证码登录：验证码正确即可登录，如果提供了密码则也验证密码（双重验证）
        if (payload.getPassword() != null && !payload.getPassword().trim().isEmpty()) {
            // 如果提供了密码，同时验证密码（双重验证，更安全）
            return authService.loginByPhone(payload.getPhone(), payload.getPassword(), ip);
        } else {
            // 仅通过验证码登录，不需要密码
            return authService.loginByPhoneWithCodeOnly(payload.getPhone(), ip);
        }
    }

    /**
     * 发送手机注册验证码
     *
     * @param payload 包含手机号的请求体
     * @return 发送结果，包括是否成功、冷却时间等信息
     */
    @PostMapping("/code/register")
    public SendCodeResponse sendRegisterCode(@Valid @RequestBody SendPhoneCodeRequest payload) {
        boolean ok = verificationCodeService.sendRegisterCode(payload.getPhone());
        String msg = ok ? "注册验证码已发送" : "发送过于频繁，请稍后再试";
        return SendCodeResponse.builder()
                .sent(ok)
                .cooldownSeconds(verificationCodeService.getSendCooldownSeconds())
                .expiresInSeconds(verificationCodeService.getCodeExpireSeconds())
                .message(msg)
                .build();
    }

    /**
     * 通过手机号和验证码进行用户注册
     *
     * @param payload 包含手机号、密码、昵称和验证码的注册请求体
     * @return 注册成功的用户信息
     * @throws Exception 注册过程中可能抛出的异常
     */
    @PostMapping("/register/phone-code")
    public AuthUserResponse registerByPhoneWithCode(@Valid @RequestBody PhoneRegisterWithCodeRequest payload) throws Exception {
        log.info("手机号+验证码注册: {}", payload.getPhone());
        boolean verified = verificationCodeService.validateRegisterAndConsume(payload.getPhone(), payload.getCode());
        if (!verified) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        return authService.registerByPhone(payload.getPhone(), payload.getPassword(), payload.getNickname());
    }

    /**
     * 用户登出
     *
     * <p>此方法负责处理用户的登出请求。它会从请求中提取认证令牌，
     * 然后从 Redis 中删除对应的会话信息，并清除客户端的认证 Cookie。
     * 这是一个双重保障，确保前端和后端都清除了登录状态。
     *
     * @param request  HTTP 请求对象，用于提取认证令牌
     * @param response HTTP 响应对象，用于清除 Cookie
     * @return 登出操作的结果
     */
    @PostMapping("/logout")
    public LogoutResponse logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        if (token != null && !token.isEmpty()) {
            try {
                stringRedisTemplate.delete("auth:token:" + token);
            } catch (Exception e) {
                log.warn("登出时删除会话失败: {}", e.getMessage());
            }
        }
        try {
            // 删除Cookie（前端也会清理，这里双保险）
            response.addHeader(HttpHeaders.SET_COOKIE, "auth_token=; Path=/; Max-Age=0");
        } catch (Exception e) {
            // ignore
        }
        return LogoutResponse.builder()
                .success(true)
                .message("已退出登录")
                .build();
    }

    /**
     * 从 HTTP 请求中提取认证令牌
     *
     * <p>此方法首先尝试从 `Authorization` 请求头中提取 "Bearer" 类型的令牌。
     * 如果请求头中没有，则会遍历请求中的所有 Cookie，查找名为 `auth_token` 的 Cookie。
     *
     * @param request HTTP 请求对象
     * @return 提取到的认证令牌，如果未找到则返回 `null`
     */
    private String extractToken(HttpServletRequest request){
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")){
                return header.substring(7);
            }
            Cookie[] cookies = request.getCookies();
            if (cookies != null){
                for (Cookie c : cookies){
                    if ("auth_token".equals(c.getName())){
                        return c.getValue();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}