package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 余票缓存版本前缀：写操作只递增 Redis 中的版本号，旧版本 key 由 TTL 自然过期，
 * 避免 @CacheEvict(allEntries=true) 一次清空全部缓存造成的击穿。
 * 非 Redis 缓存模式（dev / test）退化为本地缓存全清，保持原有语义。
 */
@Component("ticketCacheVersion")
public class TicketCacheVersion {

    private static final Logger log = LoggerFactory.getLogger(TicketCacheVersion.class);
    private static final String VERSION_KEY = "cache:tickets:version";

    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;
    private final boolean redisMode;

    public TicketCacheVersion(StringRedisTemplate redisTemplate,
                              CacheManager cacheManager,
                              @Value("${spring.cache.type:caffeine}") String cacheType) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
        this.redisMode = "redis".equalsIgnoreCase(cacheType);
    }

    /** 供 @Cacheable 的 SpEL key 使用，形如 "3:"。 */
    public String prefix() {
        if (!redisMode) return "";
        try {
            String version = redisTemplate.opsForValue().get(VERSION_KEY);
            if (version == null) {
                redisTemplate.opsForValue().setIfAbsent(VERSION_KEY, "1");
                version = redisTemplate.opsForValue().get(VERSION_KEY);
            }
            return version == null ? "" : version + ":";
        } catch (DataAccessException e) {
            log.warn("读取余票缓存版本失败，降级为无版本 key: {}", e.getMessage());
            return "";
        }
    }

    public void invalidate() {
        if (!redisMode) {
            clearTicketsCache();
            return;
        }
        try {
            redisTemplate.opsForValue().increment(VERSION_KEY);
        } catch (DataAccessException e) {
            log.warn("递增余票缓存版本失败，降级为清空 tickets 缓存: {}", e.getMessage());
            clearTicketsCache();
        }
    }

    private void clearTicketsCache() {
        Cache cache = cacheManager.getCache("tickets");
        if (cache != null) cache.clear();
    }
}
