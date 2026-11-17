package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * prod 启动校验：禁止依赖代码内置的开发密钥与口令，必须由部署环境显式提供。
 * dev / test 不加载该 bean；APP_REQUIRE_STRONG_SECRETS=false 可用于本地模拟生产。
 */
@Component
@Profile("prod")
public class ProdSecurityValidator {

    public ProdSecurityValidator(
            @Value("${app.security.require-strong-secrets:true}") boolean required,
            @Value("${spring.cache.type:caffeine}") String cacheType) {
        validate(System.getenv(), required, cacheType);
    }

    static void validate(Map<String, String> env, boolean required, String cacheType) {
        if (!required) return;
        require(env, "JWT_SECRET", 32);
        require(env, "SPRING_DATASOURCE_PASSWORD", 1);
        if ("redis".equalsIgnoreCase(cacheType)) {
            require(env, "SPRING_DATA_REDIS_PASSWORD", 1);
        }
    }

    private static void require(Map<String, String> env, String name, int minLength) {
        String value = env.get(name);
        if (value == null || value.trim().length() < minLength) {
            throw new IllegalStateException(
                    "生产环境必须通过环境变量 " + name + " 显式提供有效值（至少 " + minLength
                            + " 字符），禁止依赖内置默认值");
        }
    }
}
