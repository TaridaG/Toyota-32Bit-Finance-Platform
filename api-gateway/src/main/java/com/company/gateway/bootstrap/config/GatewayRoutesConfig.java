package com.company.gateway.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Production profili için Spring Cloud Gateway route tanımları. */
@Configuration
@Profile("!test")
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

    @Value("${gateway.services.analytics-base-uri}")
    private String analyticsBaseUri;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("v1-finance-api-public", r -> r.order(-2)
                        .path("/api/v1/public/**")
                        .uri(financeBaseUri))
                .route("v1-finance-market-overview-insights", r -> r.order(-13)
                        .path(
                                "/api/v1/market/overview", "/api/v1/market/overview/",
                                "/api/v1/market/insights", "/api/v1/market/insights/")
                        .uri(financeBaseUri))
                .route("v1-finance-market-eurobonds-tr", r -> r.order(-14)
                        .path("/api/v1/market/eurobonds/tr", "/api/v1/market/eurobonds/tr/**")
                        .uri(financeBaseUri))
                .route("v1-market-data-service-api", r -> r.order(-8)
                        .path("/api/v1/market/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("marketCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/market")))
                        .uri(marketBaseUri))
                .route("v1-market-data-service-rates", r -> r.order(-12)
                        .path("/api/v1/rates/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("marketCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/market")))
                        .uri(marketBaseUri))
                .route("v1-finance-api-news-enriched", r -> r.order(-11)
                        .path(
                                "/api/v1/news/enriched", "/api/v1/news/enriched/", "/api/v1/news/enriched/**",
                                "/api/v1/news/favorites", "/api/v1/news/favorites/", "/api/v1/news/favorites/**")
                        .uri(financeBaseUri))
                .route("v1-news-service", r -> r.order(-10)
                        .path("/api/v1/news/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("newsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/news")))
                        .uri(newsBaseUri))
                .route("v1-analytics-service", r -> r.order(-6)
                        .path("/api/v1/analytics/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("analyticsCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/analytics")))
                        .uri(analyticsBaseUri))
                .route("v1-finance-api", r -> r.order(-3)
                        .path("/api/v1/**", "/health")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(apiRateLimiter)
                                        .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(cb -> cb.setName("financeCircuitBreaker")))
                        .uri(financeBaseUri))
                .build();
    }
}
