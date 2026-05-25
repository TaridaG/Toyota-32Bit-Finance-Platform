package com.company.gateway.shared.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Gateway isteklerini method, path, status ve latency ile structured log'a yazar.
 */
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

    /** Chain tamamlandığında tek satırlık gateway_request log kaydı üretir. */
    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();

        long start = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signal -> {
            long tookMs = System.currentTimeMillis() - start;
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;

            String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID);
            if (correlationId != null && !correlationId.isBlank()) {
                MDC.put("correlationId", correlationId);
                MDC.put("traceId", correlationId);
            }
            try {
                if (status >= 400 || tookMs >= 500) {
                    log.info("gateway_request method={} path={} status={} tookMs={} outcome={}",
                            method, path, status, tookMs, signal.name());
                } else if (log.isDebugEnabled()) {
                    log.debug("gateway_request method={} path={} status={} tookMs={} outcome={}",
                            method, path, status, tookMs, signal.name());
                }
            } finally {
                MDC.clear();
            }
        });
    }

    /** CorrelationId filter'ından hemen sonra çalışacak sıra değeri. */
    @Override
    public int getOrder() {
        return -880;
    }
}