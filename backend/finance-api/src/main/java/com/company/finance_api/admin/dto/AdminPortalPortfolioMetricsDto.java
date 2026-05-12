package com.company.finance_api.admin.dto;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated external portfolio counts for admin dashboards ({@code external_portfolios}).
 */
public record AdminPortalPortfolioMetricsDto(
        long totalPortfolios,
        long newPortfoliosLast7Days,
        long newPortfoliosPrevious7Days,
        double newPortfoliosWeekOverWeekPercent,
        List<Integer> newPortfoliosDailyLast7Utc,
        /** Existing rows (created before that UTC day) with {@code updated_at} falling on that day. */
        List<Integer> portfolioUpdatesExistingDailyLast7Utc,
        Instant generatedAt,
        /** {@code false} when {@code external_portfolios} count failed — totals are placeholders, not live. */
        boolean portfolioMetricsAvailable
) {
    /** Placeholder when the live snapshot fails but the combined admin payload must still return. */
    public static AdminPortalPortfolioMetricsDto empty(Instant generatedAt) {
        List<Integer> zeros = List.of(0, 0, 0, 0, 0, 0, 0);
        return new AdminPortalPortfolioMetricsDto(
                0L,
                0L,
                0L,
                0.0,
                zeros,
                zeros,
                generatedAt,
                false);
    }
}
