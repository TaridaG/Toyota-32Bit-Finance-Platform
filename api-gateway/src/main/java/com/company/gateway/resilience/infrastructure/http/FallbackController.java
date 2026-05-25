package com.company.gateway.resilience.infrastructure.http;

import com.company.gateway.shared.filter.CorrelationIdGlobalFilter;
import com.company.gateway.shared.web.ErrorResponse;
import com.company.gateway.shared.web.GatewayErrorSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Resilience4j circuit breaker fallback endpoint'leri.
 * Upstream servis unavailable olduğunda Gateway'e kontrollü JSON yanıt döner.
 */
@RestController
public class FallbackController {

    /** finance-api circuit breaker fallback yanıtı. */
    @RequestMapping("/fallback/finance")
    public Mono<ResponseEntity<ErrorResponse>> financeFallback(ServerWebExchange exchange) {
        return Mono.just(serviceUnavailable(
                exchange,
                "FINANCE_UNAVAILABLE",
                "Finance service is temporarily unavailable"));
    }

    /** market-data-service circuit breaker fallback yanıtı. */
    @RequestMapping("/fallback/market")
    public Mono<ResponseEntity<ErrorResponse>> marketFallback(ServerWebExchange exchange) {
        return Mono.just(serviceUnavailable(
                exchange,
                "MARKET_UNAVAILABLE",
                "Market data service is temporarily unavailable"));
    }

    /** news-service circuit breaker fallback yanıtı. */
    @RequestMapping("/fallback/news")
    public Mono<ResponseEntity<ErrorResponse>> newsFallback(ServerWebExchange exchange) {
        return Mono.just(serviceUnavailable(
                exchange,
                "NEWS_UNAVAILABLE",
                "News service is temporarily unavailable"));
    }

    /** analytics-service circuit breaker fallback yanıtı. */
    @RequestMapping("/fallback/analytics")
    public Mono<ResponseEntity<ErrorResponse>> analyticsFallback(ServerWebExchange exchange) {
        return Mono.just(serviceUnavailable(
                exchange,
                "ANALYTICS_UNAVAILABLE",
                "Analytics service is temporarily unavailable"));
    }

    private static ResponseEntity<ErrorResponse> serviceUnavailable(
            ServerWebExchange exchange,
            String code,
            String message) {
        String correlationId = GatewayErrorSupport.resolveCorrelationId(exchange);
        ErrorResponse body = new ErrorResponse(code, message, Instant.now(), correlationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(CorrelationIdGlobalFilter.CORRELATION_ID, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
