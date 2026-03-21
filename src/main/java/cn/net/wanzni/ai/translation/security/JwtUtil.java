package cn.net.wanzni.ai.translation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    // For simplicity, static key; in production, load from config/ENV
    private static final SecretKey KEY = Keys.hmacShaKeyFor("translation-ai-agent-secret-key-32-bytes-min!!!!!".getBytes());

    public static String generateToken(String subject, long ttlMillis, Map<String, Object> claims) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date exp = new Date(nowMillis + ttlMillis);

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean isExpired(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null || exp.toInstant().isBefore(Instant.now());
    }
}