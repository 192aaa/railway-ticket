package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketCacheVersionTest {

    @Test
    void nonRedisModeUsesEmptyPrefixAndClearsLocalCache() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("tickets")).thenReturn(cache);

        TicketCacheVersion version = new TicketCacheVersion(
                mock(StringRedisTemplate.class), cacheManager, "caffeine");

        assertThat(version.prefix()).isEmpty();
        version.invalidate();

        verify(cache).clear();
    }
}
