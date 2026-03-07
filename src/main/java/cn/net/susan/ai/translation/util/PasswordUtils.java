package cn.net.susan.ai.translation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码处理工具类
 * 
 * 提供密码的哈希、加盐和验证功能
 * 
 * @author 苏三
 * @version 1.0.0
 */
public class PasswordUtils {
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成随机盐
     * 
     * @return Base64编码的16字节随机盐
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 对原始密码进行哈希处理
     * 
     * @param rawPassword 原始密码
     * @param salt 盐
     * @return 哈希后的密码字符串，格式为 "salt:hashedPassword"
     */
    public static String hashPassword(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] input = (salt + ":" + rawPassword).getBytes(StandardCharsets.UTF_8);
            byte[] hash = digest.digest(input);
            // 简单重复迭代提高成本
            for (int i = 0; i < 999; i++) {
                hash = digest.digest(hash);
            }
            return salt + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 验证原始密码是否与存储的哈希密码匹配
     * 
     * @param rawPassword 原始密码
     * @param stored 存储的哈希密码
     * @return 如果匹配则返回true，否则返回false
     */
    public static boolean verifyPassword(String rawPassword, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        String salt = stored.substring(0, stored.indexOf(':'));
        String rehashed = hashPassword(rawPassword, salt);
        return constantTimeEquals(stored, rehashed);
    }

    /**
     * 使用恒定时间比较两个字符串，以防止时序攻击
     * 
     * @param a 字符串a
     * @param b 字符串b
     * @return 如果两个字符串相等则返回true，否则返回false
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }
        return result == 0;
    }
}