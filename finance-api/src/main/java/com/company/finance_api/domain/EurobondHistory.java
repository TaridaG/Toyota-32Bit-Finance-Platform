package com.company.finance_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/** EurobondHistory — JPA domain entity (eurobond history). */
@Entity
@Table(name = "eurobond_history")
public class EurobondHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "isin", nullable = false, length = 12)
  private String isin;

  @Column(name = "history_date", nullable = false)
  private LocalDate historyDate;

  @Column(name = "close_price", precision = 14, scale = 6)
  private BigDecimal closePrice;

  @Column(name = "open_price", precision = 14, scale = 6)
  private BigDecimal openPrice;

  @Column(name = "high_price", precision = 14, scale = 6)
  private BigDecimal highPrice;

  @Column(name = "low_price", precision = 14, scale = 6)
  private BigDecimal lowPrice;

  @Column(name = "close_yield_percent", precision = 14, scale = 6)
  private BigDecimal closeYieldPercent;

  @Column(name = "open_yield_percent", precision = 14, scale = 6)
  private BigDecimal openYieldPercent;

  @Column(name = "high_yield_percent", precision = 14, scale = 6)
  private BigDecimal highYieldPercent;

  @Column(name = "low_yield_percent", precision = 14, scale = 6)
  private BigDecimal lowYieldPercent;

  @Column(name = "change_percent", precision = 14, scale = 6)
  private BigDecimal changePercent;

  @Column(name = "source_provider", nullable = false, length = 64)
  private String sourceProvider;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EurobondHistory() {}

  public static EurobondHistory newIngestRow(
      String isin, LocalDate historyDate, String sourceProvider) {
    EurobondHistory h = new EurobondHistory();
    h.isin = isin;
    h.historyDate = historyDate;
    h.sourceProvider = sourceProvider;
    h.createdAt = Instant.now();
    return h;
  }

  /**
   * Persists OHLC (clean, 100 nominal) and derived yields; {@code changePercent} is day-on-day move
   * of clean close vs previous business day in this series.
   */
  public void applyIngestSnapshot(
      BigDecimal openPrice,
      BigDecimal highPrice,
      BigDecimal lowPrice,
      BigDecimal closePrice,
      BigDecimal openYieldPercent,
      BigDecimal highYieldPercent,
      BigDecimal lowYieldPercent,
      BigDecimal closeYieldPercent,
      BigDecimal changePercent,
      String sourceProvider) {
    this.openPrice = openPrice;
    this.highPrice = highPrice;
    this.lowPrice = lowPrice;
    this.closePrice = closePrice;
    this.openYieldPercent = openYieldPercent;
    this.highYieldPercent = highYieldPercent;
    this.lowYieldPercent = lowYieldPercent;
    this.closeYieldPercent = closeYieldPercent;
    this.changePercent = changePercent;
    this.sourceProvider = sourceProvider;
    if (this.id == null && this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }

  public static BigDecimal bd6(Double v) {
    if (v == null || !Double.isFinite(v)) {
      return null;
    }
    return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
  }

  public String getIsin() {
    return isin;
  }

  public LocalDate getHistoryDate() {
    return historyDate;
  }

  public BigDecimal getClosePrice() {
    return closePrice;
  }

  public BigDecimal getOpenPrice() {
    return openPrice;
  }

  public BigDecimal getHighPrice() {
    return highPrice;
  }

  public BigDecimal getLowPrice() {
    return lowPrice;
  }

  public BigDecimal getCloseYieldPercent() {
    return closeYieldPercent;
  }

  public BigDecimal getOpenYieldPercent() {
    return openYieldPercent;
  }

  public BigDecimal getHighYieldPercent() {
    return highYieldPercent;
  }

  public BigDecimal getLowYieldPercent() {
    return lowYieldPercent;
  }

  public BigDecimal getChangePercent() {
    return changePercent;
  }

  public String getSourceProvider() {
    return sourceProvider;
  }
}
