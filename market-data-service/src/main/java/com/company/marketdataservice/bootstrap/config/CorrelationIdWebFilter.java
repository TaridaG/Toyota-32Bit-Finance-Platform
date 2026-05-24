package com.company.marketdataservice.bootstrap.config;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFlux isteklerinde {@code X-Correlation-Id} header'ını okur (yoksa üretir) ve SLF4J MDC'ye yazar.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    public static final String CORRELATION_ID = "correlationId";
    private static final String HEADER_NAME = "X-Correlation-Id";

    /** Gelen header veya yeni UUID ile MDC'yi doldurur; response'a correlation header ekler. */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String id = correlationId;
        exchange.getResponse().getHeaders().set(HEADER_NAME, id);
        exchange.getResponse().getHeaders().set("X-Trace-Id", id);

        return chain
                .filter(exchange)
                .doOnSubscribe(sub -> {
                    MDC.put(CORRELATION_ID, id);
                    MDC.put("traceId", id);
                })
                .doFinally(signal -> MDC.clear());
    }

    /** Security ve route filter'larından önce çalışır. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
