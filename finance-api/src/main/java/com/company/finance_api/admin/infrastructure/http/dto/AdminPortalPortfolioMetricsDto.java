package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/** Admin dashboard için external_portfolios tablosundan toplanan portfolio metrikleri. */
public record AdminPortalPortfolioMetricsDto(
    long totalPortfolios,
    long newPortfoliosLast7Days,
    long newPortfoliosPrevious7Days,
    double newPortfoliosWeekOverWeekPercent,
    List<Integer> newPortfoliosDailyLast7Utc,
    /** O UTC gününden önce oluşturulmuş ve {@code updated_at} o güne düşen satırlar. */
    List<Integer> portfolioUpdatesExistingDailyLast7Utc,
    Instant generatedAt,
    /**
     * {@code external_portfolios} count başarısızsa {@code false} — toplamlar placeholder, canlı
     * değil.
     */
    boolean portfolioMetricsAvailable) {
  /** Canlı snapshot başarısız olduğunda placeholder portfolio metrikleri üretir. */
  public static AdminPortalPortfolioMetricsDto empty(Instant generatedAt) {
    List<Integer> zeros = List.of(0, 0, 0, 0, 0, 0, 0);
    return new AdminPortalPortfolioMetricsDto(0L, 0L, 0L, 0.0, zeros, zeros, generatedAt, false);
  }
}
