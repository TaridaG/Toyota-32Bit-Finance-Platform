package com.company.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/finance")
    public Mono<ResponseEntity<Map<String, Object>>> financeFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "code", "FINANCE_UNAVAILABLE",
                        "message", "Finance service is temporarily unavailable",
                        "timestamp", Instant.now().toString()
                )));
    }

    @RequestMapping("/fallback/market")
    public Mono<ResponseEntity<Map<String, Object>>> marketFallback() {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "code", "MARKET_UNAVAILABLE",
                        "message", "Market data service is temporarily unavailable",
                        "timestamp", Instant.now().toString()
                )));
    }

    @RequestMapping("/fallback/news")
    public Mono<ResponseEntity<Map<String, Object>>> newsFallback() {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "code", "NEWS_UNAVAILABLE",
                        "message", "News service is temporarily unavailable",
                        "timestamp", Instant.now().toString()
                )));
    }

    @RequestMapping("/fallback/reporting")
    public Mono<ResponseEntity<Map<String, Object>>> reportingFallback() {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "code", "REPORTING_UNAVAILABLE",
                        "message", "Reporting service is temporarily unavailable",
                        "timestamp", Instant.now().toString()
                )));
    }

    @RequestMapping("/fallback/analytics")
    public Mono<ResponseEntity<Map<String, Object>>> analyticsFallback() {
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "code", "ANALYTICS_UNAVAILABLE",
                        "message", "Analytics service is temporarily unavailable",
                        "timestamp", Instant.now().toString()
                )));
    }
}
