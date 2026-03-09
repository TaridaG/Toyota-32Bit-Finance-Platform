package com.company.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    public GatewayRoutesConfig(RedisRateLimiter apiRateLimiter, KeyResolver userIdKeyResolver) {
        this.apiRateLimiter = apiRateLimiter;
        this.userIdKeyResolver = userIdKeyResolver;
    }
    private final RedisRateLimiter apiRateLimiter;
    private final KeyResolver userIdKeyResolver;

    @Value("${gateway.services.finance-base-uri}")
    private String financeBaseUri;

    @Value("${gateway.services.market-base-uri}")
    private String marketBaseUri;

    @Value("${gateway.services.news-base-uri}")
    private String newsBaseUri;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("finance-api", r -> r.path("/api/**", "/health")
                        .filters(f -> f
                                .preserveHostHeader()
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userIdKeyResolver)
                                )
                                .circuitBreaker(cb -> cb
                                        .setName("financeCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/finance"))
                        )
                        .uri(financeBaseUri))

                .route("market-data-service", r -> r.path("/market/**")
                        .filters(f -> f
                                .rewritePath("/market/(?<segment>.*)", "/api/market/${segment}")
                                .preserveHostHeader()
                                .circuitBreaker(cb -> cb
                                        .setName("marketCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/market"))
                        )
                        .uri(marketBaseUri))

                .route("news-service", r -> r.path("/api/news/**")
                        .filters(f -> f
                                .preserveHostHeader()
                                .circuitBreaker(cb -> cb
                                        .setName("newsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/news"))
                        )
                        .uri(newsBaseUri))

                .build();
    }
}