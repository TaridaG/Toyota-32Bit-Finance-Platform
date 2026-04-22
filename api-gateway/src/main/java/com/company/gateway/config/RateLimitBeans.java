package com.company.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RateLimitBeans {

    @Bean
    public RedisRateLimiter apiRateLimiter() {
        // replenishRate: saniyede 20 token, burstCapacity: 40
        return new RedisRateLimiter(20, 40);
    }
}