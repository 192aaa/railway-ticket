package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.cache.type=redis")
@ActiveProfiles("test")
class RedisCacheContextTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void redisCacheTypeRegistersRedisCacheManager() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(cacheManager.getCacheNames()).contains("tickets", "stations", "trains");
    }
}
