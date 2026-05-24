package com.company.finance_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** AdminMarketAssetStat — JPA domain entity (admin market asset stat). */
@Entity
@Table(name = "admin_market_asset_stat")
public class AdminMarketAssetStat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "instrument_id", nullable = false)
  private Long instrumentId;

  @Column(nullable = false, length = 64)
  private String symbol;

  @Column(name = "instrument_name", nullable = false, length = 255)
  private String instrumentName;

  @Column(name = "portfolio_count", nullable = false)
  private int portfolioCount;

  @Column(name = "user_count", nullable = false)
  private int userCount;

  @Column(name = "avg_weight_percent", nullable = false, precision = 14, scale = 4)
  private BigDecimal avgWeightPercent;

  @Column(name = "sort_rank", nullable = false)
  private int sortRank;

  protected AdminMarketAssetStat() {}

  public AdminMarketAssetStat(
      Long instrumentId,
      String symbol,
      String instrumentName,
      int portfolioCount,
      int userCount,
      BigDecimal avgWeightPercent,
      int sortRank) {
    this.instrumentId = instrumentId;
    this.symbol = symbol;
    this.instrumentName = instrumentName;
    this.portfolioCount = portfolioCount;
    this.userCount = userCount;
    this.avgWeightPercent = avgWeightPercent;
    this.sortRank = sortRank;
  }

  public Long getId() {
    return id;
  }

  public Long getInstrumentId() {
    return instrumentId;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getInstrumentName() {
    return instrumentName;
  }

  public int getPortfolioCount() {
    return portfolioCount;
  }

  public int getUserCount() {
    return userCount;
  }

  public BigDecimal getAvgWeightPercent() {
    return avgWeightPercent;
  }

  public int getSortRank() {
    return sortRank;
  }
}
