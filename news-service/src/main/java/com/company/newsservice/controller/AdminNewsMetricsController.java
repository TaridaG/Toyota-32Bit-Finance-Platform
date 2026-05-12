package com.company.newsservice.controller;

import com.company.newsservice.admin.AdminNewsAnalyticsPreset;
import com.company.newsservice.common.ApiResponse;
import com.company.newsservice.dto.AdminNewsAnalyticsDashboardDto;
import com.company.newsservice.dto.AdminNewsDashboardMetricsDto;
import com.company.newsservice.service.AdminNewsAnalyticsService;
import com.company.newsservice.service.AdminNewsMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/news/admin/metrics")
@RequiredArgsConstructor
public class AdminNewsMetricsController {

    private final AdminNewsMetricsService adminNewsMetricsService;
    private final AdminNewsAnalyticsService adminNewsAnalyticsService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminNewsDashboardMetricsDto> dashboard() {
        return ApiResponse.success(adminNewsMetricsService.snapshot());
    }

    @GetMapping("/analytics")
    public ApiResponse<AdminNewsAnalyticsDashboardDto> analytics(
            @RequestParam(name = "preset", defaultValue = "7d") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        if (hasFrom != hasTo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both 'from' and 'to' are required for a custom range.");
        }
        if (hasFrom) {
            try {
                return ApiResponse.success(adminNewsAnalyticsService.dashboardCustom(from, to));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
        }
        AdminNewsAnalyticsPreset p = AdminNewsAnalyticsPreset.parse(preset);
        return ApiResponse.success(adminNewsAnalyticsService.dashboard(p));
    }
}
