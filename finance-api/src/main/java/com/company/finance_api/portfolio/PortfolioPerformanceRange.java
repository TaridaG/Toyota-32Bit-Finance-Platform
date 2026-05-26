package com.company.finance_api.portfolio;

import java.time.LocalDate;

/** Supported preset windows for portfolio performance charts. */
public enum PortfolioPerformanceRange {
  ONE_WEEK("1w", 7),
  ONE_MONTH("1m", 30),
  THREE_MONTHS("3m", 90),
  SIX_MONTHS("6m", 180),
  ONE_YEAR("1y", 365),
  ALL("all", Integer.MAX_VALUE);

  private final String apiValue;
  private final int daysInclusive;

  PortfolioPerformanceRange(String apiValue, int daysInclusive) {
    this.apiValue = apiValue;
    this.daysInclusive = daysInclusive;
  }

  public static PortfolioPerformanceRange fromParam(String raw) {
    if (raw == null || raw.isBlank()) {
      return ONE_WEEK;
    }
    String normalized = raw.trim().toLowerCase();
    for (PortfolioPerformanceRange value : values()) {
      if (value.apiValue.equals(normalized)) {
        return value;
      }
    }
    return ONE_WEEK;
  }

  public boolean isAll() {
    return this == ALL;
  }

  public LocalDate startDayInclusive(LocalDate endDayInclusive) {
    if (isAll()) {
      return LocalDate.MIN;
    }
    return endDayInclusive.minusDays(daysInclusive - 1L);
  }
}
