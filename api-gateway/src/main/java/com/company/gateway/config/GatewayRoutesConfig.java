package com.company.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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

    @Value("${gateway.services.reporting-base-uri}")
    private String reportingBaseUri;

    @Value("${gateway.services.analytics-base-uri}")
    private String analyticsBaseUri;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("finance-api-public", r -> r.order(-1)
                        .path("/api/public/**")
                        .uri(financeBaseUri))
                // finance-api owns overview/insights only; prices, fundamentals, etc. hit MDS like before (avoids finance→MDS hop + 502s).
                .route("finance-market-overview-insights", r -> r.order(-12)
                        .path("/api/market/overview", "/api/market/overview/", "/api/market/insights", "/api/market/insights/")
                        .uri(financeBaseUri))
                .route("finance-market-eurobonds-tr", r -> r.order(-13)
                        .path("/api/market/eurobonds/tr", "/api/market/eurobonds/tr/**")
                        .uri(financeBaseUri))
                .route("market-data-service-api", r -> r.path("/api/market/**")
                .filters(f -> f
                .circuitBreaker(cb -> cb
                .setName("marketCircuitBreaker")
                .setFallbackUri("forward:/fallback/market"))
                )
                .uri(marketBaseUri))
                .route("market-data-service-rates", r -> r.order(-11)
                        .path("/api/rates/**")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName("marketCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/market"))
                        )
                        .uri(marketBaseUri))
                .route("market-data-service-legacy", r -> r.path("/market/**")
                .filters(f -> f
                .rewritePath("/market/(?<segment>.*)", "/api/market/${segment}")
                .circuitBreaker(cb -> cb
                .setName("marketCircuitBreaker")
                .setFallbackUri("forward:/fallback/market"))
                )
                .uri(marketBaseUri))
                // Must be higher priority than news-service /api/news/** (order -9).
                .route("finance-api-news-enriched", r -> r.order(-10)
                        .path(
                                "/api/news/enriched", "/api/news/enriched/", "/api/news/enriched/**",
                                "/api/news/favorites", "/api/news/favorites/", "/api/news/favorites/**"
                        )
                        .uri(financeBaseUri))
                .route("news-service", r -> r.order(-9).path("/api/news/**")
                .filters(f -> f
                .circuitBreaker(cb -> cb
                .setName("newsCircuitBreaker")
                .setFallbackUri("forward:/fallback/news"))
                )
                .uri(newsBaseUri))
                .route("reporting-service", r -> r.path("/api/reports/**")
                .filters(f -> f
                .circuitBreaker(cb -> cb
                .setName("reportingCircuitBreaker")
                .setFallbackUri("forward:/fallback/reporting"))
                )
                .uri(reportingBaseUri))
                .route("analytics-service", r -> r.path("/api/analytics/**")
                .filters(f -> f
                .circuitBreaker(cb -> cb
                .setName("analyticsCircuitBreaker")
                .setFallbackUri("forward:/fallback/analytics"))
                )
                .uri(analyticsBaseUri))
                .route("finance-api", r -> r.order(0).path("/api/**", "/health")
                .filters(f -> f
                .requestRateLimiter(rl -> rl
                .setRateLimiter(apiRateLimiter)
                .setKeyResolver(userIdKeyResolver)
                )
                .circuitBreaker(cb -> cb.setName("financeCircuitBreaker"))
                )
                .uri(financeBaseUri))
                .build();
    }
}
