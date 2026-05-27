package com.company.gateway.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * WebFlux {@link ErrorWebExceptionHandler}: downstream/proxy hatalarında standart JSON error body üretir.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;

    /** Jackson {@link ObjectMapper} ile error body serialize eder. */
    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Yakalanmamış Gateway exception'larını HTTP 502 JSON {@link ErrorResponse} olarak döner.
     */
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

        String correlationId = GatewayErrorSupport.resolveCorrelationId(exchange);

        HttpStatus status = ex instanceof NoResourceFoundException
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_GATEWAY;
        String code = status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : "GATEWAY_ERROR";

        var body = new ErrorResponse(
                code,
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                Instant.now(),
                correlationId
        );

        exchange.getResponse().setStatusCode(status);
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