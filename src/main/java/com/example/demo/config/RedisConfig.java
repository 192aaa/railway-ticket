package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 接入配置：提供 JSON 序列化的 RedisTemplate 与基于 Redis 的 Spring Cache 管理器。
 * 只有 spring.cache.type=redis 时才注册 CacheManager，dev / 测试仍可用 Caffeine / none。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer valueSerializer = jsonSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          @Value("${app.cache.redis.tickets-ttl-seconds:30}") long ticketsTtl,
                                          @Value("${app.cache.redis.stations-ttl-seconds:600}") long stationsTtl,
                                          @Value("${app.cache.redis.trains-ttl-seconds:600}") long trainsTtl) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(ticketsTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "tickets", defaults,
                        "stations", defaults.entryTtl(Duration.ofSeconds(stationsTtl)),
                        "trains", defaults.entryTtl(Duration.ofSeconds(trainsTtl))))
                .enableStatistics()
                .build();
    }

    static GenericJacksonJsonRedisSerializer jsonSerializer() {
        // 写入默认类型信息，缓存的 List<TrainTicketVO> 才能反序列化回原类型；
        // 仅用于服务端内部缓存，值不来自外部输入。
        return GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();
    }
}
