package com.company.gateway.bootstrap.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Gateway request rate limiting için {@link KeyResolver} tanımları.
 */
@Configuration
public class RateLimitConfig {

    /** Rate limit bucket anahtarını {@code X-USER-ID} veya client IP'den türetir. */
    @Bean
    public KeyResolver userIdKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-USER-ID"))
                        .filter(s -> !s.isBlank())
                        .orElseGet(() -> exchange.getRequest().getRemoteAddress() != null
                                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                                : "anonymous")
        );
    }
}