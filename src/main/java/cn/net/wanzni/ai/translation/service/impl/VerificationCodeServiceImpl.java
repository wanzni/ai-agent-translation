package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static class CodeRecord {
        String code;
        long expiresAtEpochSeconds;
        long lastSentEpochSeconds;
        boolean consumed;
    }

    private final ConcurrentHashMap<String, CodeRecord> store = new ConcurrentHashMap<>();

    private static final int CODE_EXPIRE_SECONDS = 300; // 5分钟
    private static final int SEND_COOLDOWN_SECONDS = 60; // 60秒

    /**
     * 发送用于登录的手机验证码。
     * @param phone 手机号
     * @return 发送是否成功
     */
    @Override
    public boolean sendLoginCode(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        phone = phone.trim();

        long now = Instant.now().getEpochSecond();
        String key = phone + "|LOGIN";
        CodeRecord existing = store.get(key);
        if (existing != null) {
            long sinceLast = now - existing.lastSentEpochSeconds;
            if (sinceLast < SEND_COOLDOWN_SECONDS) {
                log.info("发送验证码冷却中: phone={}, remain={}s", phone, (SEND_COOLDOWN_SECONDS - sinceLast));
                return false;
            }
        }

        String code = generate6DigitCode();
        CodeRecord record = new CodeRecord();
        record.code = code;
        record.lastSentEpochSeconds = now;
        record.expiresAtEpochSeconds = now + CODE_EXPIRE_SECONDS;
        record.consumed = false;
        store.put(key, record);

        // 此处应调用短信服务商发送短信；当前为演示日志输出
        log.info("[DEV] 已生成登录验证码: phone={}, code={}, expire={}s", phone, code, CODE_EXPIRE_SECONDS);
        return true;
    }

    /**
     * 校验并消耗登录验证码（防止重复使用）。
     * @param phone 手机号
     * @param code 验证码
     * @return 验证是否通过
     */
    @Override
    public boolean validateAndConsume(String phone, String code) {
        if (phone == null || code == null) return false;
        phone = phone.trim();
        code = code.trim();
        CodeRecord record = store.get(phone + "|LOGIN");
        if (record == null) return false;
        long now = Instant.now().getEpochSecond();
        if (record.consumed) return false;
        if (now > record.expiresAtEpochSeconds) return false;
        if (!code.equals(record.code)) return false;
        record.consumed = true;
        return true;
    }

    /**
     * 获取发送冷却秒数（再次发送需等待的时间）。
     * @return 冷却时间的秒数
     */
    @Override
    public int getSendCooldownSeconds() {
        return SEND_COOLDOWN_SECONDS;
    }

    /**
     * 获取验证码有效期秒数。
     * @return 验证码的有效秒数
     */
    @Override
    public int getCodeExpireSeconds() {
        return CODE_EXPIRE_SECONDS;
    }

    /**
     * 发送用于注册的手机验证码（与登录验证码相互独立）。
     * @param phone 手机号
     * @return 发送是否成功
     */
    @Override
    public boolean sendRegisterCode(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        phone = phone.trim();
        long now = Instant.now().getEpochSecond();
        String key = phone + "|REGISTER";
        CodeRecord existing = store.get(key);
        if (existing != null) {
            long sinceLast = now - existing.lastSentEpochSeconds;
            if (sinceLast < SEND_COOLDOWN_SECONDS) {
                log.info("注册验证码冷却中: phone={}, remain={}s", phone, (SEND_COOLDOWN_SECONDS - sinceLast));
                return false;
            }
        }
        String code = generate6DigitCode();
        CodeRecord record = new CodeRecord();
        record.code = code;
        record.lastSentEpochSeconds = now;
        record.expiresAtEpochSeconds = now + CODE_EXPIRE_SECONDS;
        record.consumed = false;
        store.put(key, record);
        log.info("[DEV] 已生成注册验证码: phone={}, code={}, expire={}s", phone, code, CODE_EXPIRE_SECONDS);
        return true;
    }

    /**
     * 校验并消耗注册验证码。
     * @param phone 手机号
     * @param code 验证码
     * @return 验证是否通过
     */
    @Override
    public boolean validateRegisterAndConsume(String phone, String code) {
        if (phone == null || code == null) return false;
        phone = phone.trim();
        code = code.trim();
        CodeRecord record = store.get(phone + "|REGISTER");
        if (record == null) return false;
        long now = Instant.now().getEpochSecond();
        if (record.consumed) return false;
        if (now > record.expiresAtEpochSeconds) return false;
        if (!code.equals(record.code)) return false;
        record.consumed = true;
        return true;
    }

    /**
     * 生成6位数字验证码
     *
     * @return 6位数字验证码字符串
     */
    private String generate6DigitCode() {
        Random r = new Random();
        int n = 100000 + r.nextInt(900000);
        return String.valueOf(n);
    }
}