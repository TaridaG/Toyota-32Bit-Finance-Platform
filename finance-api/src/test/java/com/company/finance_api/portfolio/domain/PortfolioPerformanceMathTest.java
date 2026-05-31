package com.company.finance_api.portfolio.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PortfolioPerformanceMathTest {

  @Test
  void dailyReturnPct_removes_same_day_buy_flow_from_performance() {
    BigDecimal dailyReturnPct =
        PortfolioPerformanceMath.dailyReturnPct(
            new BigDecimal("100.00"), new BigDecimal("150.00"), new BigDecimal("50.00"));

    assertThat(dailyReturnPct).isEqualByComparingTo("0.000000");
  }

  @Test
  void dailyReturnPct_captures_market_move_after_flow_adjustment() {
    BigDecimal dailyReturnPct =
        PortfolioPerformanceMath.dailyReturnPct(
            new BigDecimal("100.00"), new BigDecimal("155.00"), new BigDecimal("50.00"));

    assertThat(dailyReturnPct).isEqualByComparingTo("5.000000");
  }

  @Test
  void advanceTwrIndex_chain_links_daily_returns() {
    BigDecimal index = PortfolioPerformanceMath.advanceTwrIndex(null, new BigDecimal("5.000000"));
    index = PortfolioPerformanceMath.advanceTwrIndex(index, new BigDecimal("-2.000000"));

    assertThat(index).isEqualByComparingTo("1.0290000000");
    assertThat(PortfolioPerformanceMath.twrPctFromIndex(index)).isEqualByComparingTo("2.900000");
  }

  @Test
  void dailyReturnPct_is_null_without_prior_market_value() {
    assertThat(
            PortfolioPerformanceMath.dailyReturnPct(
                BigDecimal.ZERO, new BigDecimal("120.00"), new BigDecimal("120.00")))
        .isNull();
  }
}
