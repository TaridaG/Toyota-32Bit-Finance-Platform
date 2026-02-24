package com.company.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping(value = "/fallback/finance", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> financeFallback() {
        return Mono.just(Map.of(
                "code", "FINANCE_UNAVAILABLE",
                "message", "Finance service is temporarily unavailable",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping(value = "/fallback/market", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> marketFallback() {
        return Mono.just(Map.of(
                "code", "MARKET_UNAVAILABLE",
                "message", "Market data service is temporarily unavailable",
                "timestamp", Instant.now().toString()
        ));
    }
}