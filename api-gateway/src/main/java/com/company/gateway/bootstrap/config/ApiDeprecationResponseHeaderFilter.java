package com.company.gateway.bootstrap.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Marks unversioned {@code /api/**} responses as deprecated; clients should migrate to
 * {@link ApiVersionPathSupport#EXTERNAL_API_VERSION_PREFIX}.
 */
@Component
public class ApiDeprecationResponseHeaderFilter implements GlobalFilter, Ordered {

    static final String DEPRECATION_HEADER = "Deprecation";
    static final String SUNSET_HEADER = "Sunset";
    static final String LINK_HEADER = "Link";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String path = exchange.getRequest().getURI().getPath();
            if (!ApiVersionPathSupport.isLegacyApiPath(path)) {
                return;
            }
            HttpHeaders headers = exchange.getResponse().getHeaders();
            if (!headers.containsKey(DEPRECATION_HEADER)) {
                headers.add(DEPRECATION_HEADER, "true");
            }
            if (!headers.containsKey(LINK_HEADER)) {
                String successor = path.replaceFirst("^/api/", ApiVersionPathSupport.EXTERNAL_API_VERSION_PREFIX + "/");
                headers.add(LINK_HEADER, "<" + successor + ">; rel=\"successor-version\"");
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
