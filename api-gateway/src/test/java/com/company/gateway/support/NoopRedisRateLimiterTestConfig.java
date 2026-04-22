package com.company.gateway.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Avoids a real Redis connection during {@code @SpringBootTest}; the gateway request rate limiter
 * would otherwise block requests when Redis is unavailable.
 */
@TestConfiguration
@Profile("test")
public class NoopRedisRateLimiterTestConfig {

    @Bean
    @Primary
    public RedisRateLimiter apiRateLimiter() {
        return Mockito.mock(RedisRateLimiter.class, invocation -> {
            if ("isAllowed".equals(invocation.getMethod().getName())) {
                return Mono.just(new RateLimiter.Response(true, Collections.emptyMap()));
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
    }
}
