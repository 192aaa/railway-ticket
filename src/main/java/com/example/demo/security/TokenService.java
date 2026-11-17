package com.example.demo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * JWT 登出黑名单与一次性 refresh token，均存放在 Redis 并按剩余有效期自动过期。
 * 非 Redis 环境（dev / test）降级为不启用，登录流程仍可用 access token。
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final long refreshExpirationMs;
    private final boolean redisMode;

    public TokenService(StringRedisTemplate redisTemplate,
                        JwtUtil jwtUtil,
                        @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs,
                        @Value("${spring.cache.type:caffeine}") String cacheType) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.refreshExpirationMs = refreshExpirationMs;
        this.redisMode = "redis".equalsIgnoreCase(cacheType);
    }

    public String issueRefreshToken(Long userId) {
        if (!redisMode || userId == null) return null;
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            redisTemplate.opsForValue().set(REFRESH_PREFIX + token, String.valueOf(userId),
                    Duration.ofMillis(refreshExpirationMs));
            return token;
        } catch (DataAccessException e) {
            log.warn("写入 refresh token 失败: {}", e.getMessage());
            return null;
        }
    }

    /** 校验并立即作废旧 refresh token（轮换），避免重放。 */
    public Long consumeRefreshToken(String refreshToken) {
        if (!redisMode || refreshToken == null || refreshToken.isBlank()) return null;
        String key = REFRESH_PREFIX + refreshToken;
        try {
            String userId = redisTemplate.opsForValue().get(key);
            if (userId == null) return null;
            redisTemplate.delete(key);
            return Long.valueOf(userId);
        } catch (DataAccessException | NumberFormatException e) {
            log.warn("校验 refresh token 失败: {}", e.getMessage());
            return null;
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        if (!redisMode || refreshToken == null || refreshToken.isBlank()) return;
        try {
            redisTemplate.delete(REFRESH_PREFIX + refreshToken);
        } catch (DataAccessException e) {
            log.warn("撤销 refresh token 失败: {}", e.getMessage());
        }
    }

    public void blacklist(String accessToken) {
        if (!redisMode || accessToken == null || accessToken.isBlank()) return;
        long ttl = jwtUtil.getRemainingMillis(accessToken);
        if (ttl <= 0) return;
        try {
            // 只存指纹，避免把完整 JWT 明文写入 Redis
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + fingerprint(accessToken), "1",
                    Duration.ofMillis(ttl));
        } catch (DataAccessException e) {
            log.warn("写入 token 黑名单失败: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String accessToken) {
        if (!redisMode || accessToken == null || accessToken.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + fingerprint(accessToken)));
        } catch (DataAccessException e) {
            log.warn("查询 token 黑名单失败，放行本次请求: {}", e.getMessage());
            return false;
        }
    }

    private String fingerprint(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
