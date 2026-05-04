package com.company.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestGatewayRoutesConfig {

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
                .route("market-data-service-api", r -> r.path("/api/market/**")
                        .uri(marketBaseUri))
                .route("market-data-service-legacy", r -> r.path("/market/**")
                        .filters(f -> f
                                .rewritePath("/market/(?<segment>.*)", "/api/market/${segment}"))
                        .uri(marketBaseUri))
                .route("news-service", r -> r.path("/api/news/**")
                        .uri(newsBaseUri))
                .route("reporting-service", r -> r.path("/api/reports/**")
                        .uri(reportingBaseUri))
                .route("analytics-service", r -> r.path("/api/analytics/**")
                        .uri(analyticsBaseUri))
                .route("finance-api", r -> r.path("/api/**", "/health")
                        .uri(financeBaseUri))
                .build();
    }
}
