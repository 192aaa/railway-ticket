package com.example.demo.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的短 TTL 互斥锁，保证多实例部署时定时任务只有一个实例执行。
 * 仅在 spring.cache.type=redis 时生效；dev / test / 无 Redis 的部署直接放行，不依赖 Redis。
 */
@Component
public class RedisTaskLock {

    private static final Logger log = LoggerFactory.getLogger(RedisTaskLock.class);

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public RedisTaskLock(StringRedisTemplate redisTemplate,
                         @Value("${spring.cache.type:caffeine}") String cacheType) {
        this.redisTemplate = redisTemplate;
        this.enabled = "redis".equalsIgnoreCase(cacheType);
    }

    public boolean tryLock(String key, String owner, Duration ttl) {
        if (!enabled) return true;
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, owner, ttl));
        } catch (DataAccessException e) {
            // Redis 故障时让任务继续跑，比所有实例都不执行更可取
            log.warn("Redis 不可用，定时任务降级为本地执行: {}", e.getMessage());
            return true;
        }
    }

    public void unlock(String key, String owner) {
        if (!enabled) return;
        try {
            // 只释放自己持有的锁，避免 TTL 过期后误删其他实例的锁
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), owner);
        } catch (DataAccessException e) {
            log.warn("释放 Redis 任务锁失败，将等待 TTL 自动过期: {}", e.getMessage());
        }
    }
}
