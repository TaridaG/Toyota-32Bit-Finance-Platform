package com.company.finance_api.admin.dto;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated portal user metrics for admin dashboards (sourced from {@code users} table).
 */
public record AdminPortalUserMetricsDto(
        long totalUsers,
        long newUsersLast7Days,
        long newUsersPrevious7Days,
        double newUsersWeekOverWeekPercent,
        List<Integer> newRegistrationsDailyLast7Utc,
        Instant generatedAt
) {
}
