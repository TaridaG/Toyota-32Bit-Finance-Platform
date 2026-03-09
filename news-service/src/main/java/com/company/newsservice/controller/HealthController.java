package com.company.newsservice.controller;

import com.company.newsservice.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "service", "news-service",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}