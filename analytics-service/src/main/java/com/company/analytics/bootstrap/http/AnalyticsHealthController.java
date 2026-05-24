package com.company.analytics.bootstrap.http;

import com.company.analytics.shared.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Analytics servisi sağlık kontrolü REST endpoint'i. */
@RestController
public class AnalyticsHealthController {

    /** Servis adı ve durum bilgisini döner. */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "service", "analytics-service",
                "status", "UP"
        ));
    }
}