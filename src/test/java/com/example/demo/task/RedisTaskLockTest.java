package com.example.demo.task;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisTaskLockTest {

    @Test
    void skipsLockingWhenRedisCacheIsNotUsed() {
        RedisTaskLock lock = new RedisTaskLock(mock(StringRedisTemplate.class), "caffeine");

        assertThat(lock.tryLock("task:test", "owner", Duration.ofSeconds(5))).isTrue();
        lock.unlock("task:test", "owner");
    }
}
