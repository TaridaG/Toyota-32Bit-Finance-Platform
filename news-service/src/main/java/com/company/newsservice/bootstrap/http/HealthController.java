package com.company.newsservice.bootstrap.http;

import com.company.newsservice.shared.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Container/liveness probe için basit health endpoint'i.
 */
@RestController
public class HealthController {

    /** Servis adı, durum ve timestamp ile {@link ApiResponse} döner. */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "service", "news-service",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}