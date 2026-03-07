package cn.net.susan.ai.translation.payment;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 二维码预下单结果
 * 封装二维码内容（codeUrl）与有效期（expiresAt）。
 * 前端使用 codeUrl 生成二维码，超过 expiresAt 需重新下单。
 */
@Getter
public class PaymentQrPrepayResult {
    private final String codeUrl;
    private final LocalDateTime expiresAt;

    public PaymentQrPrepayResult(String codeUrl, LocalDateTime expiresAt) {
        this.codeUrl = codeUrl;
        this.expiresAt = expiresAt;
    }
    // Lombok @Getter 提供访问器
}