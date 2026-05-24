package com.company.gateway.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proxies each downstream service OpenAPI document to {@code /services/{name}/v3/api-docs} so the
 * gateway Swagger UI can aggregate every microservice spec.
 */
@Configuration
public class GatewayOpenApiRoutesConfig {

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

    @Value("${gateway.services.log-consumer-base-uri}")
    private String logConsumerBaseUri;

    @Value("${gateway.services.notification-base-uri}")
    private String notificationBaseUri;

    @Bean
    public RouteLocator openApiRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("openapi-finance", r -> r.order(-100)
                        .path("/services/finance/v3/api-docs", "/services/finance/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/finance/(?<path>.*)", "/${path}"))
                        .uri(financeBaseUri))
                .route("openapi-market", r -> r.order(-100)
                        .path("/services/market/v3/api-docs", "/services/market/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/market/(?<path>.*)", "/${path}"))
                        .uri(marketBaseUri))
                .route("openapi-news", r -> r.order(-100)
                        .path("/services/news/v3/api-docs", "/services/news/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/news/(?<path>.*)", "/${path}"))
                        .uri(newsBaseUri))
                .route("openapi-reporting", r -> r.order(-100)
                        .path("/services/reporting/v3/api-docs", "/services/reporting/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/reporting/(?<path>.*)", "/${path}"))
                        .uri(reportingBaseUri))
                .route("openapi-analytics", r -> r.order(-100)
                        .path("/services/analytics/v3/api-docs", "/services/analytics/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/analytics/(?<path>.*)", "/${path}"))
                        .uri(analyticsBaseUri))
                .route("openapi-log-consumer", r -> r.order(-100)
                        .path("/services/log-consumer/v3/api-docs", "/services/log-consumer/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/log-consumer/(?<path>.*)", "/${path}"))
                        .uri(logConsumerBaseUri))
                .route("openapi-notification", r -> r.order(-100)
                        .path("/services/notification/v3/api-docs", "/services/notification/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/services/notification/(?<path>.*)", "/${path}"))
                        .uri(notificationBaseUri))
                .build();
    }
}
