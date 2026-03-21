package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.dto.auth.AuthUserResponse;
import cn.net.wanzni.ai.translation.dto.auth.LoginResponse;

/**
 * 用户认证服务接口，提供用户注册和登录功能。
 */
public interface AuthService {
    /**
     * 通过手机号注册新用户。
     *
     * @param phone 手机号
     * @param password 密码
     * @param nickname 昵称
     * @return 注册成功的用户信息
     * @throws Exception 注册失败时抛出异常
     */
    AuthUserResponse registerByPhone(String phone, String password, String nickname) throws Exception;

    /**
     * 通过手机号和密码登录。
     *
     * @param phone 手机号
     * @param password 密码
     * @param ipAddress 登录IP地址
     * @return 登录成功后的响应，包含用户信息和token
     * @throws Exception 登录失败时抛出异常
     */
    LoginResponse loginByPhone(String phone, String password, String ipAddress) throws Exception;

    /**
     * 通过手机号和验证码登录（不需要密码）。
     *
     * @param phone 手机号
     * @param ipAddress 登录IP地址
     * @return 登录成功后的响应，包含用户信息和token
     * @throws Exception 登录失败时抛出异常
     */
    LoginResponse loginByPhoneWithCodeOnly(String phone, String ipAddress) throws Exception;
}