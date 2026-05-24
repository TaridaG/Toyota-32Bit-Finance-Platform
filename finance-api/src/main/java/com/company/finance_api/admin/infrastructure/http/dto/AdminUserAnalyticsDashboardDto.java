package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/** Enterprise 'Total users' admin analytics dashboard payload'u (UTC). */
public record AdminUserAnalyticsDashboardDto(
    String preset,
    Instant chartRangeStartUtcInclusive,
    Instant chartRangeEndUtcExclusive,
    AdminUserAnalyticsSummaryDto summary,
    List<AdminUserDailyRegistrationDto> dailyRegistrations,
    List<AdminRecentRegistrationRowDto> recentRegistrations,
    AdminUserInteractionMetricsDto interaction,
    AdminUserSegmentsDto segments,
    AdminUserHeatmapDto heatmap,
    Instant generatedAt) {}
