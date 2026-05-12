package com.company.finance_api.admin.dto;

/**
 * Product analytics (sessions, page views) — populated only when a telemetry pipeline exists.
 */
public record AdminUserInteractionMetricsDto(
        boolean measured,
        Long averageSessionDurationSeconds,
        Long sessionCount,
        Long pageViews,
        Double bounceRatePercent
) {
    public static AdminUserInteractionMetricsDto notMeasured() {
        return new AdminUserInteractionMetricsDto(false, null, null, null, null);
    }
}
