package com.company.newsservice.admin.infrastructure.http;

import com.company.newsservice.admin.domain.AdminNewsAnalyticsPreset;
import com.company.newsservice.shared.web.ApiResponse;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsAnalyticsDashboardDto;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsDashboardMetricsDto;
import com.company.newsservice.admin.application.BuildAdminNewsAnalyticsUseCase;
import com.company.newsservice.admin.application.GetAdminNewsDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Admin news metrics HTTP endpoint'leri ({@code /api/news/admin/metrics}).
 */
@RestController
@RequestMapping("/api/news/admin/metrics")
@RequiredArgsConstructor
public class AdminNewsMetricsController {

    private final GetAdminNewsDashboardUseCase adminNewsMetricsService;
    private final BuildAdminNewsAnalyticsUseCase adminNewsAnalyticsService;

    /** Dashboard snapshot endpoint'i. */
    @GetMapping("/dashboard")
    public ApiResponse<AdminNewsDashboardMetricsDto> dashboard() {
        return ApiResponse.success(adminNewsMetricsService.snapshot());
    }

    /** Preset veya custom range ile analytics dashboard endpoint'i. */
    @GetMapping("/analytics")
    public ApiResponse<AdminNewsAnalyticsDashboardDto> analytics(
            @RequestParam(name = "preset", defaultValue = "7d") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        if (hasFrom != hasTo) {
            throw new IllegalArgumentException("Both 'from' and 'to' are required for a custom range.");
        }
        if (hasFrom) {
            return ApiResponse.success(adminNewsAnalyticsService.dashboardCustom(from, to));
        }
        AdminNewsAnalyticsPreset p = AdminNewsAnalyticsPreset.parse(preset);
        return ApiResponse.success(adminNewsAnalyticsService.dashboard(p));
    }
}
