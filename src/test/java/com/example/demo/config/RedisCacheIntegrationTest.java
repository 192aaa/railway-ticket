package com.example.demo.config;

import com.example.demo.dto.TrainTicketVO;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.RateLimitFilter;
import com.example.demo.security.TokenService;
import com.example.demo.service.TicketCacheVersion;
import com.example.demo.task.RedisTaskLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 Redis 冒烟：带密码连接、缓存序列化 / TTL / 版本化失效、任务锁、令牌黑名单与限流。
 * 需要 Docker；CI（ubuntu-latest）会执行，无 Docker 的本地环境自动跳过。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class RedisCacheIntegrationTest {

    private static final String PASSWORD = "test_redis_pwd";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", PASSWORD);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> PASSWORD);
        registry.add("management.health.redis.enabled", () -> "false");
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTaskLock taskLock;

    @Autowired
    private TicketCacheVersion ticketCacheVersion;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    /** 每个用例使用独立 key，避免共享 Redis 容器中的测试间顺序依赖。 */
    @BeforeEach
    void cleanTestKeys() {
        for (String key : List.of("smoke:key", "tickets::smoke", "tickets::smoke:v:")) {
            redisTemplate.delete(key);
        }
        redisTemplate.delete(redisTemplate.keys("tickets::smoke:*"));
    }

    @Test
    void redisTemplateReadsAndWritesWithPassword() {
        redisTemplate.opsForValue().set("smoke:key", "value", Duration.ofSeconds(30));

        assertThat(redisTemplate.opsForValue().get("smoke:key")).isEqualTo("value");
        assertThat(redisTemplate.getExpire("smoke:key")).isPositive();
    }

    @Test
    void cacheValueRoundTripsAndEvictsFromRedis() {
        Cache cache = cacheManager.getCache("tickets");
        assertThat(cache).isNotNull();
        cache.put("smoke", "hello");

        // 用 get(key) 拿 ValueWrapper，让 Jackson 依据 JSON 内嵌 @class 还原类型；
        // get(key, Object.class) 会把值误当 type id 解析，导致偶发 cache miss
        Cache.ValueWrapper wrapper = cache.get("smoke");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.get()).isEqualTo("hello");
        assertThat(redisTemplate.hasKey("tickets::smoke")).isTrue();
        assertThat(redisTemplate.getExpire("tickets::smoke")).isBetween(1L, 30L);

        cache.evict("smoke");
        assertThat(redisTemplate.hasKey("tickets::smoke")).isFalse();
        assertThat(cache.get("smoke")).isNull();

        // 复杂对象（含 @class 类型信息）的序列化往返由 redisTemplate 单独验证
        TrainTicketVO vo = new TrainTicketVO();
        vo.setTrainId(1L);
        vo.setTrainNumber("G1");
        redisTemplate.opsForValue().set("tickets::smoke:vo", new ArrayList<>(List.of(vo)), Duration.ofSeconds(30));
        Object restored = redisTemplate.opsForValue().get("tickets::smoke:vo");
        assertThat(restored).isInstanceOf(List.class);
        assertThat(((List<?>) restored).get(0)).isInstanceOf(TrainTicketVO.class);
    }

    @Test
    void versionBumpInvalidatesWithoutClearingExistingKeys() {
        Cache cache = cacheManager.getCache("tickets");
        assertThat(cache).isNotNull();

        String oldKey = ticketCacheVersion.prefix() + "A_B_2026-12-01";
        cache.put(oldKey, new ArrayList<>(List.of("old")));
        assertThat(cache.get(oldKey)).isNotNull();

        ticketCacheVersion.invalidate();

        String newKey = ticketCacheVersion.prefix() + "A_B_2026-12-01";
        assertThat(newKey).isNotEqualTo(oldKey);
        // 旧 key 未被删除，由 30s TTL 自然过期，不会因一次写操作清空整个缓存
        assertThat(cache.get(oldKey)).isNotNull();
        assertThat(cache.get(newKey)).isNull();
    }

    @Test
    void taskLockIsExclusiveAndOnlyOwnerCanRelease() {
        String key = "task:smoke:lock";
        String first = "owner-a";
        String second = "owner-b";

        assertThat(taskLock.tryLock(key, first, Duration.ofSeconds(10))).isTrue();
        assertThat(taskLock.tryLock(key, second, Duration.ofSeconds(10))).isFalse();

        taskLock.unlock(key, second);
        assertThat(taskLock.tryLock(key, second, Duration.ofSeconds(10))).isFalse();

        taskLock.unlock(key, first);
        assertThat(taskLock.tryLock(key, second, Duration.ofSeconds(10))).isTrue();
        taskLock.unlock(key, second);
    }

    @Test
    void refreshTokenIsOneTimeAndLogoutBlacklistsAccessToken() {
        String refresh = tokenService.issueRefreshToken(1L);
        assertThat(refresh).isNotBlank();
        assertThat(tokenService.consumeRefreshToken(refresh)).isEqualTo(1L);
        assertThat(tokenService.consumeRefreshToken(refresh)).isNull();

        String access = jwtUtil.generateToken(1L, "smoke");
        assertThat(tokenService.isBlacklisted(access)).isFalse();
        tokenService.blacklist(access);
        assertThat(tokenService.isBlacklisted(access)).isTrue();
    }

    @Test
    void rateLimitReturns429AfterThreshold() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilter(new MockHttpServletRequest("POST", "/api/user/login"),
                    response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        rateLimitFilter.doFilter(new MockHttpServletRequest("POST", "/api/user/login"),
                blocked, new MockFilterChain());
        assertThat(blocked.getStatus()).isEqualTo(429);
    }
}
