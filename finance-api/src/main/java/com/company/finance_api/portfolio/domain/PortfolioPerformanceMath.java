package com.company.finance_api.portfolio.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Utility math helpers for daily portfolio performance series. */
public final class PortfolioPerformanceMath {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal EPS = new BigDecimal("0.000001");

  private PortfolioPerformanceMath() {}

  public static BigDecimal dailyPnl(
      BigDecimal previousMarketValue, BigDecimal marketValue, BigDecimal netFlow) {
    return nz(marketValue).subtract(nz(previousMarketValue)).subtract(nz(netFlow)).setScale(6, RoundingMode.HALF_UP);
  }

  public static BigDecimal dailyReturnPct(
      BigDecimal previousMarketValue, BigDecimal marketValue, BigDecimal netFlow) {
    if (previousMarketValue == null || previousMarketValue.abs().compareTo(EPS) < 0) {
      return null;
    }
    return dailyPnl(previousMarketValue, marketValue, netFlow)
        .multiply(ONE_HUNDRED)
        .divide(previousMarketValue, 6, RoundingMode.HALF_UP);
  }

  public static BigDecimal advanceTwrIndex(BigDecimal previousIndex, BigDecimal dailyReturnPct) {
    BigDecimal base = previousIndex != null ? previousIndex : BigDecimal.ONE;
    if (dailyReturnPct == null) {
      return base.setScale(10, RoundingMode.HALF_UP);
    }
    return base.multiply(
            BigDecimal.ONE.add(dailyReturnPct.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)))
        .setScale(10, RoundingMode.HALF_UP);
  }

  public static BigDecimal twrPctFromIndex(BigDecimal twrIndex) {
    BigDecimal index = twrIndex != null ? twrIndex : BigDecimal.ONE;
    return index.subtract(BigDecimal.ONE).multiply(ONE_HUNDRED).setScale(6, RoundingMode.HALF_UP);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }
}
