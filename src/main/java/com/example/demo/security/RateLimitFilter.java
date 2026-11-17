package com.example.demo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 基于 Redis 的固定窗口限流，覆盖登录/注册（防暴力破解）与余票查询（防刷接口）。
 * 仅在 spring.cache.type=redis 时启用；Redis 故障时降级放行，不阻断业务。
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final DefaultRedisScript<Long> COUNTER_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) " +
                    "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                    "return c",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final long loginMax;
    private final long registerMax;
    private final long queryMax;
    private final long windowSeconds;

    public RateLimitFilter(StringRedisTemplate redisTemplate,
                           @Value("${spring.cache.type:caffeine}") String cacheType,
                           @Value("${app.rate-limit.login-max:10}") long loginMax,
                           @Value("${app.rate-limit.register-max:10}") long registerMax,
                           @Value("${app.rate-limit.query-max:60}") long queryMax,
                           @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.enabled = "redis".equalsIgnoreCase(cacheType);
        this.loginMax = loginMax;
        this.registerMax = registerMax;
        this.queryMax = queryMax;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Limit limit = limitFor(request.getRequestURI());
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate:" + limit.name() + ":" + clientIp(request);
        try {
            Long count = redisTemplate.execute(COUNTER_SCRIPT, List.of(key), String.valueOf(windowSeconds));
            if (count != null && count > limit.max()) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
                return;
            }
        } catch (DataAccessException e) {
            log.warn("限流计数失败，放行本次请求: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private Limit limitFor(String path) {
        if (path.startsWith("/api/user/login")) return new Limit("login", loginMax);
        if (path.startsWith("/api/user/register")) return new Limit("register", registerMax);
        if (path.startsWith("/api/ticket/query")) return new Limit("query", queryMax);
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Limit(String name, long max) {}
}
