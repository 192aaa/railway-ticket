package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TokenServiceTest {

    @Test
    void nonRedisModeNeverTouchesRedis() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        TokenService service = new TokenService(template, mock(JwtUtil.class), 604800000L, "caffeine");

        assertThat(service.issueRefreshToken(1L)).isNull();
        assertThat(service.consumeRefreshToken("refresh")).isNull();
        assertThat(service.isBlacklisted("jwt")).isFalse();
        service.blacklist("jwt");
        service.revokeRefreshToken("refresh");

        verifyNoInteractions(template);
    }
}
