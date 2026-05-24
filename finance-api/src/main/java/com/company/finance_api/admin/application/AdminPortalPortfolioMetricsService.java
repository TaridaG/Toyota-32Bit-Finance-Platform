package com.company.finance_api.admin.application;

import com.company.finance_api.admin.infrastructure.http.dto.AdminPortalPortfolioMetricsDto;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * External portfolio toplamları ve oluşturma hızını admin dashboard için UTC bucket'larında
 * hesaplar.
 */
@Service
public class AdminPortalPortfolioMetricsService {

  private static final Logger log =
      LoggerFactory.getLogger(AdminPortalPortfolioMetricsService.class);

  private final ExternalPortfolioRepository externalPortfolioRepository;

  public AdminPortalPortfolioMetricsService(
      ExternalPortfolioRepository externalPortfolioRepository) {
    this.externalPortfolioRepository = externalPortfolioRepository;
  }

  /** Anlık portfolio metrics snapshot DTO'sunu üretir. */
  @Transactional(readOnly = true)
  public AdminPortalPortfolioMetricsDto snapshot() {
    Instant now = Instant.now();
    long total = 0L;
    try {
      total = externalPortfolioRepository.count();
    } catch (RuntimeException ex) {
      log.error("external_portfolios count() failed", ex);
      return AdminPortalPortfolioMetricsDto.empty(now);
    }

    long last7 = 0L;
    long prev7 = 0L;
    List<Integer> daily = List.of(0, 0, 0, 0, 0, 0, 0);
    List<Integer> updatesDaily = List.of(0, 0, 0, 0, 0, 0, 0);
    try {
      Instant sevenAgo = now.minus(7, ChronoUnit.DAYS);
      Instant fourteenAgo = now.minus(14, ChronoUnit.DAYS);
      last7 = countCreatedBetweenInstants(sevenAgo, now);
      prev7 = countCreatedBetweenInstants(fourteenAgo, sevenAgo);
      daily = dailyNewPortfoliosUtcLast7Days(now);
      updatesDaily = dailyExistingPortfolioUpdatesUtcLast7Days(now);
    } catch (RuntimeException ex) {
      log.warn(
          "Portfolio velocity/daily buckets skipped; totalPortfolios={} is still valid", total, ex);
    }

    double wow = AdminPortalUserMetricsService.weekOverWeekPercent(last7, prev7);
    return new AdminPortalPortfolioMetricsDto(
        total, last7, prev7, wow, daily, updatesDaily, now, true);
  }

  private long countCreatedBetweenInstants(Instant from, Instant to) {
    LocalDateTime start = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
    LocalDateTime end = LocalDateTime.ofInstant(to, ZoneOffset.UTC);
    return externalPortfolioRepository.countCreatedBetween(
        Timestamp.valueOf(start), Timestamp.valueOf(end));
  }

  private List<Integer> dailyNewPortfoliosUtcLast7Days(Instant now) {
    LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
    List<Integer> out = new ArrayList<>(7);
    for (int i = 6; i >= 0; i--) {
      LocalDate d = todayUtc.minusDays(i);
      LocalDateTime start = d.atStartOfDay(ZoneOffset.UTC).toLocalDateTime();
      LocalDateTime end = start.plusDays(1);
      out.add(
          (int)
              Math.min(
                  Integer.MAX_VALUE,
                  externalPortfolioRepository.countCreatedBetween(
                      Timestamp.valueOf(start), Timestamp.valueOf(end))));
    }
    return out;
  }

  private List<Integer> dailyExistingPortfolioUpdatesUtcLast7Days(Instant now) {
    LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
    List<Integer> out = new ArrayList<>(7);
    for (int i = 6; i >= 0; i--) {
      LocalDate d = todayUtc.minusDays(i);
      LocalDateTime dayStart = d.atStartOfDay(ZoneOffset.UTC).toLocalDateTime();
      LocalDateTime dayEnd = dayStart.plusDays(1);
      out.add(
          (int)
              Math.min(
                  Integer.MAX_VALUE,
                  externalPortfolioRepository.countExistingUpdatedOnUtcDay(
                      Timestamp.valueOf(dayStart), Timestamp.valueOf(dayEnd))));
    }
    return out;
  }
}
