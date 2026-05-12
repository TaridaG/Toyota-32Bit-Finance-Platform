package com.company.finance_api.admin.dto;

import java.time.Instant;
import java.util.List;

/**
 * Admin-only analytics payload for the “Total users” enterprise dashboard (UTC).
 */
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
        Instant generatedAt
) {
}
