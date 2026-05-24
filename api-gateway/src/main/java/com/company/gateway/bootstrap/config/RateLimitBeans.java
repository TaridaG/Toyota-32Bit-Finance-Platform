package com.company.gateway.bootstrap.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Production Redis tabanlı Gateway rate limiter bean'leri.
 */
@Configuration
@Profile("!test")
public class RateLimitBeans {

    /** finance-api catch-all route için saniyede 20 istek, burst 40 token bucket limiti. */
    @Bean
    public RedisRateLimiter apiRateLimiter() {
        // replenishRate: saniyede 20 token, burstCapacity: 40
        return new RedisRateLimiter(20, 40);
    }
}