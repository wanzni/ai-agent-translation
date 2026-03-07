package cn.net.susan.ai.translation.service;

/**
 * 验证码服务接口，提供手机验证码的发送和校验功能。
 */
public interface VerificationCodeService {
    /**
     * 发送用于登录的手机验证码。
     * @param phone 手机号
     * @return 发送是否成功
     */
    boolean sendLoginCode(String phone);

    /**
     * 校验并消耗登录验证码（防止重复使用）。
     * @param phone 手机号
     * @param code 验证码
     * @return 验证是否通过
     */
    boolean validateAndConsume(String phone, String code);

    /**
     * 获取发送冷却秒数（再次发送需等待的时间）。
     * @return 冷却时间的秒数
     */
    int getSendCooldownSeconds();

    /**
     * 获取验证码有效期秒数。
     * @return 验证码的有效秒数
     */
    int getCodeExpireSeconds();

    /**
     * 发送用于注册的手机验证码（与登录验证码相互独立）。
     * @param phone 手机号
     * @return 发送是否成功
     */
    boolean sendRegisterCode(String phone);

    /**
     * 校验并消耗注册验证码。
     * @param phone 手机号
     * @param code 验证码
     * @return 验证是否通过
     */
    boolean validateRegisterAndConsume(String phone, String code);
}