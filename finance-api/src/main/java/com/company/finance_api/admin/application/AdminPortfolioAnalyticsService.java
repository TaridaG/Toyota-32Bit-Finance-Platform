package com.company.finance_api.admin.application;

import com.company.finance_api.admin.domain.AdminUserAnalyticsPreset;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortfolioAnalyticsDashboardDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortfolioAnalyticsSummaryDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortfolioDailyCreationDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminRecentPortfolioRowDto;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPositionLotRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enterprise 'Active portfolios' analytics: oluşturma hızı, pozisyon derinliği ve roster kapsamı
 * (UTC).
 */
@Service
public class AdminPortfolioAnalyticsService {

  private static final ZoneOffset UTC = ZoneOffset.UTC;
  public static final int MAX_CUSTOM_RANGE_DAYS = 120;
  private static final int RECENT_LIMIT = 25;

  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final ExternalPositionLotRepository externalPositionLotRepository;
  private final UserRepository userRepository;

  public AdminPortfolioAnalyticsService(
      ExternalPortfolioRepository externalPortfolioRepository,
      ExternalPositionLotRepository externalPositionLotRepository,
      UserRepository userRepository) {
    this.externalPortfolioRepository = externalPortfolioRepository;
    this.externalPositionLotRepository = externalPositionLotRepository;
    this.userRepository = userRepository;
  }

  /** Preset pencere için portfolio analytics dashboard DTO'sunu üretir. */
  @Transactional(readOnly = true)
  public AdminPortfolioAnalyticsDashboardDto dashboard(AdminUserAnalyticsPreset preset) {
    Instant now = Instant.now();
    LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
    int spanDays = preset.inclusiveDayCount();
    LocalDate lastChartDay = todayUtc;
    LocalDate firstChartDay = todayUtc.minusDays(spanDays - 1);
    return build(firstChartDay, lastChartDay, preset.queryParam(), now);
  }

  /** Özel UTC tarih aralığı için dashboard DTO'sunu üretir. */
  @Transactional(readOnly = true)
  public AdminPortfolioAnalyticsDashboardDto dashboardCustom(
      LocalDate fromInclusive, LocalDate toInclusive) {
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
      throw new IllegalArgumentException(
          "range must not exceed " + MAX_CUSTOM_RANGE_DAYS + " days");
    }
    if (first.isBefore(todayUtc.minusYears(5))) {
      throw new IllegalArgumentException("from is too far in the past");
    }
    return build(first, last, "custom", now);
  }

  private AdminPortfolioAnalyticsDashboardDto build(
      LocalDate firstChartDay, LocalDate lastChartDay, String presetKey, Instant now) {
    LocalDate todayUtc = LocalDate.ofInstant(now, UTC);
    int span = (int) ChronoUnit.DAYS.between(firstChartDay, lastChartDay) + 1;

    Instant chartFrom = firstChartDay.atStartOfDay(UTC).toInstant();
    Instant chartToExclusive = lastChartDay.plusDays(1).atStartOfDay(UTC).toInstant();

    long totalPortfolios = externalPortfolioRepository.count();
    long openLots = externalPositionLotRepository.countByDeletedFalse();
    double averageOpenLotsPerPortfolio =
        totalPortfolios > 0 ? (double) openLots / totalPortfolios : 0.0;
    long rosterUsers = userRepository.countByNotPendingDeletion();
    double portfoliosPerRosterUser = rosterUsers > 0 ? (double) totalPortfolios / rosterUsers : 0.0;

    LocalDate mondayThisUtcWeek = todayUtc.with(DayOfWeek.MONDAY);
    LocalDate prevIsoWeekStart = mondayThisUtcWeek.minusWeeks(1);
    LocalDateTime prevIsoWeekStartLdt = prevIsoWeekStart.atStartOfDay(UTC).toLocalDateTime();
    LocalDateTime mondayThisWeekLdt = mondayThisUtcWeek.atStartOfDay(UTC).toLocalDateTime();
    long prevWeekCreated =
        externalPortfolioRepository.countCreatedBetween(
            Timestamp.valueOf(prevIsoWeekStartLdt), Timestamp.valueOf(mondayThisWeekLdt));

    YearMonth prevMonth = YearMonth.from(todayUtc).minusMonths(1);
    LocalDateTime prevMonthFrom = prevMonth.atDay(1).atStartOfDay(UTC).toLocalDateTime();
    LocalDateTime prevMonthToExclusive =
        prevMonth.plusMonths(1).atDay(1).atStartOfDay(UTC).toLocalDateTime();
    long prevMonthCreated =
        externalPortfolioRepository.countCreatedBetween(
            Timestamp.valueOf(prevMonthFrom), Timestamp.valueOf(prevMonthToExclusive));

    LocalDate y = todayUtc.minusDays(1);
    LocalDateTime yStart = y.atStartOfDay(UTC).toLocalDateTime();
    LocalDateTime yEnd = y.plusDays(1).atStartOfDay(UTC).toLocalDateTime();
    LocalDate y2 = todayUtc.minusDays(2);
    LocalDateTime y2Start = y2.atStartOfDay(UTC).toLocalDateTime();
    LocalDateTime y2End = y2.plusDays(1).atStartOfDay(UTC).toLocalDateTime();
    long createdYesterday =
        externalPortfolioRepository.countCreatedBetween(
            Timestamp.valueOf(yStart), Timestamp.valueOf(yEnd));
    long createdDayBefore =
        externalPortfolioRepository.countCreatedBetween(
            Timestamp.valueOf(y2Start), Timestamp.valueOf(y2End));

    long impliedPrior = Math.max(0L, totalPortfolios - createdYesterday);
    double totalVsPrior =
        AdminPortalUserMetricsService.weekOverWeekPercent(totalPortfolios, impliedPrior);

    AdminPortfolioAnalyticsSummaryDto summary =
        new AdminPortfolioAnalyticsSummaryDto(
            totalPortfolios,
            prevWeekCreated,
            prevMonthCreated,
            averageOpenLotsPerPortfolio,
            portfoliosPerRosterUser,
            rosterUsers,
            openLots,
            createdYesterday,
            createdDayBefore,
            totalVsPrior);

    List<Integer> dailyCounts = new ArrayList<>(span);
    for (int i = 0; i < span; i++) {
      LocalDate d = firstChartDay.plusDays(i);
      LocalDateTime start = d.atStartOfDay(UTC).toLocalDateTime();
      LocalDateTime end = start.plusDays(1);
      dailyCounts.add(
          (int)
              Math.min(
                  Integer.MAX_VALUE,
                  externalPortfolioRepository.countCreatedBetween(
                      Timestamp.valueOf(start), Timestamp.valueOf(end))));
    }

    List<Double> rolling = new ArrayList<>(span);
    for (int i = 0; i < span; i++) {
      int from = Math.max(0, i - 6);
      double sum = 0;
      int n = 0;
      for (int j = from; j <= i; j++) {
        sum += dailyCounts.get(j);
        n++;
      }
      rolling.add(n > 0 ? sum / n : 0.0);
    }

    List<AdminPortfolioDailyCreationDto> daily = new ArrayList<>(span);
    for (int i = 0; i < span; i++) {
      LocalDate d = firstChartDay.plusDays(i);
      int c = dailyCounts.get(i);
      int prev = i > 0 ? dailyCounts.get(i - 1) : 0;
      int delta = i > 0 ? c - prev : 0;
      daily.add(new AdminPortfolioDailyCreationDto(d.toString(), c, rolling.get(i), delta));
    }

    List<ExternalPortfolio> recentRows =
        externalPortfolioRepository.findAllWithUserOrderByCreatedAtDesc(
            PageRequest.of(0, RECENT_LIMIT));
    List<AdminRecentPortfolioRowDto> recent = new ArrayList<>(recentRows.size());
    DateTimeFormatter createdFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    for (ExternalPortfolio p : recentRows) {
      String owner =
          p.getUser() != null && p.getUser().getUsername() != null
              ? p.getUser().getUsername()
              : "—";
      recent.add(
          new AdminRecentPortfolioRowDto(
              p.getId(),
              p.getName(),
              p.getBaseCurrency(),
              owner,
              p.getCreatedAt() == null ? null : p.getCreatedAt().format(createdFmt)));
    }

    return new AdminPortfolioAnalyticsDashboardDto(
        presetKey, chartFrom, chartToExclusive, summary, daily, recent, now);
  }
}
