package com.company.gateway.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is served by finance-api (springdoc MVC). Gateway only proxies the UI and webjars;
 * aggregated OpenAPI JSON stays on {@link GatewayOpenApiRoutesConfig}.
 */
@Configuration
public class GatewaySwaggerUiRoutesConfig {

    @Value("${gateway.services.finance-base-uri}")
    private String financeBaseUri;

    @Bean
    public RouteLocator swaggerUiRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("swagger-ui", r -> r.order(-200)
                        .path("/swagger-ui/**", "/swagger-ui.html")
                        .uri(financeBaseUri))
                .route("swagger-webjars", r -> r.order(-200)
                        .path("/webjars/**")
                        .uri(financeBaseUri))
                .route("swagger-api-docs", r -> r.order(-200)
                        .path("/v3/api-docs", "/v3/api-docs/**")
                        .uri(financeBaseUri))
                .build();
    }
}
