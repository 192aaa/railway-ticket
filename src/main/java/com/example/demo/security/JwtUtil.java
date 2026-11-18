package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import com.example.demo.entity.UserRole;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 兼容旧调用：签发普通用户令牌。 */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, UserRole.USER);
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        String safeRole = UserRole.isValid(role) ? role : UserRole.USER;

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", safeRole)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /** 从令牌读取角色；旧令牌无 role 声明时按普通用户处理。 */
    public String getRole(String token) {
        String role = parseToken(token).get("role", String.class);
        return UserRole.isValid(role) ? role : UserRole.USER;
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** token 剩余有效毫秒数，用于设置登出黑名单的 TTL。 */
    public long getRemainingMillis(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            return 0;
        }
    }
}