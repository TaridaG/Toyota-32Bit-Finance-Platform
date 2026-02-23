package com.company.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Value("${gateway.services.finance-base-uri}")
    private String financeBaseUri;

    @Value("${gateway.services.market-base-uri}")
    private String marketBaseUri;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("finance-api", r -> r.path("/api/**", "/health")
                        .filters(f -> f
                                .preserveHostHeader()
                                .removeRequestHeader("X-USER-ID") // spoof engeli
                        )
                        .uri(financeBaseUri))

                .route("market-data-service", r -> r.path("/market/**")
                        .filters(f -> f
                                .rewritePath("/market/(?<segment>.*)", "/api/market/${segment}")
                                .preserveHostHeader()
                        )
                        .uri(marketBaseUri))

                .build();
    }
}