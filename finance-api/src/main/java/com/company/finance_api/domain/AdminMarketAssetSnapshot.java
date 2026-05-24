package com.company.finance_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** AdminMarketAssetSnapshot — JPA domain entity (admin market asset snapshot). */
@Entity
@Table(name = "admin_market_asset_snapshot")
public class AdminMarketAssetSnapshot {

  public static final long SINGLETON_ID = 1L;

  @Id private Long id = SINGLETON_ID;

  @Column(nullable = false, length = 20)
  private String status = "IDLE";

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "computed_at")
  private Instant computedAt;

  @Column(name = "avg_watchlist_instruments_per_user", precision = 14, scale = 4)
  private BigDecimal avgWatchlistInstrumentsPerUser;

  @Column(name = "avg_instruments_per_portfolio", precision = 14, scale = 4)
  private BigDecimal avgInstrumentsPerPortfolio;

  @Column(name = "avg_portfolio_weight_percent", precision = 14, scale = 4)
  private BigDecimal avgPortfolioWeightPercent;

  @Column(name = "instrument_row_count", nullable = false)
  private int instrumentRowCount;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  protected AdminMarketAssetSnapshot() {}

  public Long getId() {
    return id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getComputedAt() {
    return computedAt;
  }

  public void setComputedAt(Instant computedAt) {
    this.computedAt = computedAt;
  }

  public BigDecimal getAvgWatchlistInstrumentsPerUser() {
    return avgWatchlistInstrumentsPerUser;
  }

  public void setAvgWatchlistInstrumentsPerUser(BigDecimal avgWatchlistInstrumentsPerUser) {
    this.avgWatchlistInstrumentsPerUser = avgWatchlistInstrumentsPerUser;
  }

  public BigDecimal getAvgInstrumentsPerPortfolio() {
    return avgInstrumentsPerPortfolio;
  }

  public void setAvgInstrumentsPerPortfolio(BigDecimal avgInstrumentsPerPortfolio) {
    this.avgInstrumentsPerPortfolio = avgInstrumentsPerPortfolio;
  }

  public BigDecimal getAvgPortfolioWeightPercent() {
    return avgPortfolioWeightPercent;
  }

  public void setAvgPortfolioWeightPercent(BigDecimal avgPortfolioWeightPercent) {
    this.avgPortfolioWeightPercent = avgPortfolioWeightPercent;
  }

  public int getInstrumentRowCount() {
    return instrumentRowCount;
  }

  public void setInstrumentRowCount(int instrumentRowCount) {
    this.instrumentRowCount = instrumentRowCount;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
