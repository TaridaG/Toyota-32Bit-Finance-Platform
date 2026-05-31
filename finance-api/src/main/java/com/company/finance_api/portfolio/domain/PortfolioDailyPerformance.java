package com.company.finance_api.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Precomputed daily portfolio value and performance row for charting. */
@Entity
@Table(name = "portfolio_daily_performance")
@Getter
@Setter
public class PortfolioDailyPerformance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "external_portfolio_id", nullable = false)
  private Long externalPortfolioId;

  @Column(nullable = false, length = 8)
  private String currency;

  @Column(name = "day_utc", nullable = false)
  private LocalDate dayUtc;

  @Column(name = "market_value", nullable = false, precision = 19, scale = 6)
  private BigDecimal marketValue;

  @Column(name = "net_flow", nullable = false, precision = 19, scale = 6)
  private BigDecimal netFlow;

  @Column(name = "daily_pnl", nullable = false, precision = 19, scale = 6)
  private BigDecimal dailyPnl;

  @Column(name = "daily_return_pct", precision = 19, scale = 6)
  private BigDecimal dailyReturnPct;

  @Column(name = "twr_index", nullable = false, precision = 19, scale = 10)
  private BigDecimal twrIndex;

  @Column(nullable = false)
  private boolean complete;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;
}
