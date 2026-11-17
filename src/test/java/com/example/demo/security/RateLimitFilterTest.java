package com.example.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RateLimitFilterTest {

    @Test
    void nonRedisModePassesThroughWithoutTouchingRedis() throws Exception {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        RateLimitFilter filter = new RateLimitFilter(template, "caffeine", 10, 10, 60, 60);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/user/login"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(template);
    }
}
