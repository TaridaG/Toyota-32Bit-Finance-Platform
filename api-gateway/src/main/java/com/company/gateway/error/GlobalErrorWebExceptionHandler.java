package com.company.gateway.error;

import com.company.gateway.filter.CorrelationIdGlobalFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        if (log.isDebugEnabled()) {
            log.debug("Gateway error", ex);
        } else {
            log.warn("Gateway error: {} ({})", ex.getMessage(), ex.getClass().getName());
        }

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

        var body = new ErrorResponse(
                "GATEWAY_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                Instant.now(),
                correlationId
        );

        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.BAD_GATEWAY);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}