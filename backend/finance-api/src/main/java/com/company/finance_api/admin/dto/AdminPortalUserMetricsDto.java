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
        /** Users who started account deletion in that UTC day (parallel to {@link #newRegistrationsDailyLast7Utc}). */
        List<Integer> userDeletionRequestsDailyLast7Utc,
        Instant generatedAt
) {
}
