package com.example.demo.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdSecurityValidatorTest {

    private static final String JWT = "a".repeat(40);

    @Test
    void checkCanBeDisabledForLocalProdSimulation() {
        assertThatCode(() -> ProdSecurityValidator.validate(Map.of(), false, "redis"))
                .doesNotThrowAnyException();
    }

    @Test
    void missingJwtSecretIsRejected() {
        assertThatThrownBy(() -> ProdSecurityValidator.validate(Map.of(), true, "redis"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void redisModeAlsoRequiresRedisPassword() {
        Map<String, String> env = Map.of(
                "JWT_SECRET", JWT,
                "SPRING_DATASOURCE_PASSWORD", "db-password");

        assertThatThrownBy(() -> ProdSecurityValidator.validate(env, true, "redis"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_DATA_REDIS_PASSWORD");
    }

    @Test
    void explicitSecretsPassValidation() {
        Map<String, String> env = Map.of(
                "JWT_SECRET", JWT,
                "SPRING_DATASOURCE_PASSWORD", "db-password",
                "SPRING_DATA_REDIS_PASSWORD", "redis-password");

        assertThatCode(() -> ProdSecurityValidator.validate(env, true, "redis"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ProdSecurityValidator.validate(
                Map.of("JWT_SECRET", JWT, "SPRING_DATASOURCE_PASSWORD", "db-password"),
                true, "caffeine")).doesNotThrowAnyException();
    }
}
