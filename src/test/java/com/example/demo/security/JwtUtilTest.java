package com.example.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test_only_secret_key_for_jwt_signing_at_least_32_bytes");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
    }

    @Test
    void generateTokenShouldCarryUserIdAndUsername() {
        String token = jwtUtil.generateToken(42L, "zhangsan");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUsername(token)).isEqualTo("zhangsan");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void validateTokenShouldRejectTamperedToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateTokenShouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken(1L, "admin");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}
