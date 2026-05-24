package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/**
 * Tek admin-dashboard payload: portal kullanıcı + external portfolio metrikleri (tek HTTP
 * round-trip).
 */
public record AdminPortalDashboardMetricsDto(
    long totalUsers,
    long newUsersLast7Days,
    long newUsersPrevious7Days,
    double newUsersWeekOverWeekPercent,
    List<Integer> newRegistrationsDailyLast7Utc,
    List<Integer> userDeletionRequestsDailyLast7Utc,
    long totalPortfolios,
    long newPortfoliosLast7Days,
    long newPortfoliosPrevious7Days,
    double newPortfoliosWeekOverWeekPercent,
    List<Integer> newPortfoliosDailyLast7Utc,
    List<Integer> portfolioUpdatesExistingDailyLast7Utc,
    Instant generatedAt,
    boolean portfolioMetricsAvailable,
    long totalInstruments,
    List<Integer> instrumentDistinctWithPriceDailyLast7Utc) {
  /** Kullanıcı ve portfolio snapshot'larını tek dashboard DTO'sunda birleştirir. */
  public static AdminPortalDashboardMetricsDto from(
      AdminPortalUserMetricsDto users,
      AdminPortalPortfolioMetricsDto portfolios,
      long totalInstruments,
      List<Integer> instrumentDistinctWithPriceDailyLast7Utc) {
    return new AdminPortalDashboardMetricsDto(
        users.totalUsers(),
        users.newUsersLast7Days(),
        users.newUsersPrevious7Days(),
        users.newUsersWeekOverWeekPercent(),
        users.newRegistrationsDailyLast7Utc(),
        users.userDeletionRequestsDailyLast7Utc(),
        portfolios.totalPortfolios(),
        portfolios.newPortfoliosLast7Days(),
        portfolios.newPortfoliosPrevious7Days(),
        portfolios.newPortfoliosWeekOverWeekPercent(),
        portfolios.newPortfoliosDailyLast7Utc(),
        portfolios.portfolioUpdatesExistingDailyLast7Utc(),
        users.generatedAt(),
        portfolios.portfolioMetricsAvailable(),
        totalInstruments,
        instrumentDistinctWithPriceDailyLast7Utc);
  }
}
