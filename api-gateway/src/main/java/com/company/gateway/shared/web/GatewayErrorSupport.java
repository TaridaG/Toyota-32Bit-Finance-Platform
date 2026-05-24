package com.company.gateway.shared.web;

import com.company.gateway.shared.filter.CorrelationIdGlobalFilter;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

public final class GatewayErrorSupport {

    private GatewayErrorSupport() {
    }

    public static String resolveCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        if (!exchange.getResponse().isCommitted()) {
            exchange.getResponse().getHeaders().set(CorrelationIdGlobalFilter.CORRELATION_ID, correlationId);
        }
        return correlationId;
    }
}
