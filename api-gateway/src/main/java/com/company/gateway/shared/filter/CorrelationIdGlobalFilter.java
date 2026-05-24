package com.company.gateway.shared.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Her Gateway isteğine {@code X-Correlation-Id} header'ı ekleyen global filter.
 * İstemciden gelen correlation id yoksa yeni UUID üretir.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -1000;

    public static final String CORRELATION_ID = "X-Correlation-Id";

    /**
     * Request/response header'larına correlation id yazar ve downstream chain'e iletir.
     */
    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(CORRELATION_ID, correlationId)
                .build();

        if (!exchange.getResponse().isCommitted()) {
            exchange.getResponse().getHeaders().set(CORRELATION_ID, correlationId);
        }

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /** Filter sırası: security ve logging filter'larından önce çalışır. */
    @Override
    public int getOrder() {
        return ORDER;
    }
}