package com.company.finance_api.admin;

import com.company.finance_api.admin.dto.AdminRecentRegistrationRowDto;
import com.company.finance_api.admin.dto.AdminUserAnalyticsDashboardDto;
import com.company.finance_api.admin.dto.AdminUserAnalyticsSummaryDto;
import com.company.finance_api.admin.dto.AdminUserDailyRegistrationDto;
import com.company.finance_api.admin.dto.AdminUserHeatmapDto;
import com.company.finance_api.admin.dto.AdminUserInteractionMetricsDto;
import com.company.finance_api.admin.dto.AdminUserSegmentsDto;
import com.company.finance_api.domain.User;
import com.company.finance_api.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserAnalyticsService {

    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final int RECENT_LIMIT = 15;
    /** Maximum inclusive day span for custom ranges (abuse guard). */
    public static final int MAX_CUSTOM_RANGE_DAYS = 120;

    private final UserRepository userRepository;

    public AdminUserAnalyticsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminUserAnalyticsDashboardDto dashboard(AdminUserAnalyticsPreset preset) {
        Instant now = Instant.now();
        LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
        int spanDays = preset.inclusiveDayCount();
        LocalDate lastChartDay = todayUtc;
        LocalDate firstChartDay = todayUtc.minusDays(spanDays - 1);
        return build(firstChartDay, lastChartDay, preset.queryParam(), now);
    }

    /**
     * Inclusive UTC calendar dates {@code [fromInclusive, toInclusive]}, capped at today UTC.
     *
     * @throws IllegalArgumentException when the range is empty, inverted, too long, or too far in the past
     */
    @Transactional(readOnly = true)
    public AdminUserAnalyticsDashboardDto dashboardCustom(LocalDate fromInclusive, LocalDate toInclusive) {
        Instant now = Instant.now();
        LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
        if (fromInclusive == null || toInclusive == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        LocalDate last = toInclusive.isAfter(todayUtc) ? todayUtc : toInclusive;
        LocalDate first = fromInclusive;
        long spanLong = ChronoUnit.DAYS.between(first, last) + 1;
        if (spanLong < 1) {
            throw new IllegalArgumentException("invalid range");
        }
        if (spanLong > MAX_CUSTOM_RANGE_DAYS) {
            throw new IllegalArgumentException("range must not exceed " + MAX_CUSTOM_RANGE_DAYS + " days");
        }
        if (first.isBefore(todayUtc.minusYears(5))) {
            throw new IllegalArgumentException("from is too far in the past");
        }
        return build(first, last, "custom", now);
    }

    private AdminUserAnalyticsDashboardDto build(
            LocalDate firstChartDay, LocalDate lastChartDay, String presetKey, Instant now) {
        LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
        int span = (int) ChronoUnit.DAYS.between(firstChartDay, lastChartDay) + 1;

        Instant chartFrom = firstChartDay.atStartOfDay(UTC).toInstant();
        Instant chartToExclusive = lastChartDay.plusDays(1).atStartOfDay(UTC).toInstant();

        long totalUsers = userRepository.countByNotPendingDeletion();
        long activeUsers = userRepository.countActiveRoster();
        double activeRate = totalUsers > 0 ? 100.0 * activeUsers / totalUsers : 0.0;

        Instant sevenAgo = now.minus(7, ChronoUnit.DAYS);
        Instant fourteenAgo = now.minus(14, ChronoUnit.DAYS);
        long last7 = userRepository.countCreatedInRangeExcludingPendingDeletion(sevenAgo, now);
        long prev7 = userRepository.countCreatedInRangeExcludingPendingDeletion(fourteenAgo, sevenAgo);
        double wow7 = AdminPortalUserMetricsService.weekOverWeekPercent(last7, prev7);

        LocalDate prevPeriodFirst = firstChartDay.minusDays(span);
        Instant prevFrom = prevPeriodFirst.atStartOfDay(UTC).toInstant();
        Instant prevToExclusive = firstChartDay.atStartOfDay(UTC).toInstant();
        long currentPeriod = userRepository.countCreatedInRangeExcludingPendingDeletion(chartFrom, chartToExclusive);
        long previousPeriod = userRepository.countCreatedInRangeExcludingPendingDeletion(prevFrom, prevToExclusive);
        double growthPeriod = AdminPortalUserMetricsService.weekOverWeekPercent(currentPeriod, previousPeriod);

        LocalDate y = todayUtc.minusDays(1);
        Instant yStart = y.atStartOfDay(UTC).toInstant();
        Instant yEnd = y.plusDays(1).atStartOfDay(UTC).toInstant();
        LocalDate y2 = todayUtc.minusDays(2);
        Instant y2Start = y2.atStartOfDay(UTC).toInstant();
        Instant y2End = y2.plusDays(1).atStartOfDay(UTC).toInstant();
        long newUsersYesterday = userRepository.countCreatedInRangeExcludingPendingDeletion(yStart, yEnd);
        long newUsersDayBefore = userRepository.countCreatedInRangeExcludingPendingDeletion(y2Start, y2End);

        LocalDate mondayThisUtcWeek = todayUtc.with(DayOfWeek.MONDAY);
        LocalDate prevIsoWeekStart = mondayThisUtcWeek.minusWeeks(1);
        Instant prevIsoWeekFrom = prevIsoWeekStart.atStartOfDay(UTC).toInstant();
        Instant prevIsoWeekToExclusive = mondayThisUtcWeek.atStartOfDay(UTC).toInstant();
        long newUsersPreviousIsoWeekUtc =
                userRepository.countCreatedInRangeExcludingPendingDeletion(prevIsoWeekFrom, prevIsoWeekToExclusive);

        YearMonth prevMonth = YearMonth.from(todayUtc).minusMonths(1);
        Instant prevMonthFrom = prevMonth.atDay(1).atStartOfDay(UTC).toInstant();
        Instant prevMonthToExclusive = prevMonth.plusMonths(1).atDay(1).atStartOfDay(UTC).toInstant();
        long newUsersPreviousCalendarMonthUtc =
                userRepository.countCreatedInRangeExcludingPendingDeletion(prevMonthFrom, prevMonthToExclusive);

        long impliedRosterPriorUtcDay = Math.max(0L, totalUsers - newUsersYesterday);
        double totalUsersVsPriorDayPercentApprox = AdminPortalUserMetricsService.weekOverWeekPercent(
                totalUsers, impliedRosterPriorUtcDay);

        List<Integer> newCounts = new ArrayList<>(span);
        List<Integer> delCounts = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate d = firstChartDay.plusDays(i);
            Instant start = d.atStartOfDay(UTC).toInstant();
            Instant end = start.plus(1, ChronoUnit.DAYS);
            newCounts.add((int) Math.min(Integer.MAX_VALUE, userRepository.countCreatedInRangeExcludingPendingDeletion(start, end)));
            delCounts.add((int) Math.min(Integer.MAX_VALUE, userRepository.countDeletionRequestedInRange(start, end)));
        }

        List<Double> rolling = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            int from = Math.max(0, i - 6);
            double sum = 0;
            int n = 0;
            for (int j = from; j <= i; j++) {
                sum += newCounts.get(j);
                n++;
            }
            rolling.add(n > 0 ? sum / n : 0.0);
        }

        List<AdminUserDailyRegistrationDto> daily = new ArrayList<>(span);
        Integer todayNew = null;
        for (int i = 0; i < span; i++) {
            LocalDate d = firstChartDay.plusDays(i);
            int nu = newCounts.get(i);
            int prevNu = i > 0 ? newCounts.get(i - 1) : 0;
            int delta = i > 0 ? nu - prevNu : 0;
            daily.add(new AdminUserDailyRegistrationDto(
                    d.toString(),
                    nu,
                    delCounts.get(i),
                    rolling.get(i),
                    delta));
            if (d.equals(todayUtc)) {
                todayNew = nu;
            }
        }

        AdminUserAnalyticsSummaryDto summary = new AdminUserAnalyticsSummaryDto(
                totalUsers,
                last7,
                prev7,
                wow7,
                last7 - prev7,
                currentPeriod,
                previousPeriod,
                growthPeriod,
                activeUsers,
                activeRate,
                newUsersYesterday,
                newUsersDayBefore,
                newUsersYesterday - newUsersDayBefore,
                todayNew,
                newUsersPreviousIsoWeekUtc,
                newUsersPreviousCalendarMonthUtc,
                totalUsersVsPriorDayPercentApprox);

        List<AdminRecentRegistrationRowDto> recent = mapRecent();

        return new AdminUserAnalyticsDashboardDto(
                presetKey,
                chartFrom,
                chartToExclusive,
                summary,
                daily,
                recent,
                AdminUserInteractionMetricsDto.notMeasured(),
                AdminUserSegmentsDto.notTracked(),
                AdminUserHeatmapDto.notTracked(),
                now);
    }

    private List<AdminRecentRegistrationRowDto> mapRecent() {
        List<User> rows = userRepository.findRosterByCreatedAtDesc(PageRequest.of(0, RECENT_LIMIT));
        List<AdminRecentRegistrationRowDto> out = new ArrayList<>(rows.size());
        for (User u : rows) {
            String status = u.isActive() ? "ACTIVE" : "INACTIVE";
            out.add(new AdminRecentRegistrationRowDto(
                    u.getId(),
                    u.getUsername(),
                    maskEmail(u.getEmail()),
                    u.getCreatedAt(),
                    "",
                    "",
                    status));
        }
        return out;
    }

    static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "—";
        }
        int at = email.indexOf('@');
        if (at < 1) {
            return "—";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        if (local.length() <= 2) {
            return "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}
