package com.company.finance_api.admin.dto;

/**
 * KPI block derived from {@code users} (roster + creation timestamps).
 * Rolling 7d fields use wall-clock windows; week/month headline counts use UTC calendar boundaries (see field docs).
 */
public record AdminUserAnalyticsSummaryDto(
        long totalUsers,
        long newUsersLast7Days,
        long newUsersPrevious7Days,
        double newUsersWeekOverWeekPercent,
        long userDeltaLast7VsPrev7,
        long newUsersCurrentPeriod,
        long newUsersPreviousPeriod,
        double growthPercentPeriodVsPrevious,
        long activeUsers,
        double activeRatePercent,
        long newUsersYesterday,
        long newUsersDayBeforeYesterday,
        long yesterdayVsPriorDayDelta,
        Integer todayNewUsersUtc,
        /** New registrations in the previous ISO week (Mon 00:00 UTC → next Mon 00:00 exclusive), excluding pending deletion. */
        long newUsersPreviousIsoWeekUtc,
        /** New registrations in the previous UTC calendar month (first day 00:00 → next month first day 00:00 exclusive). */
        long newUsersPreviousCalendarMonthUtc,
        /**
         * Approximate roster day-over-day percent: {@code totalUsers} vs {@code totalUsers - newUsersYesterday},
         * treating yesterday's net adds as new signups only (ignores same-day removals from roster).
         */
        double totalUsersVsPriorDayPercentApprox
) {
}
