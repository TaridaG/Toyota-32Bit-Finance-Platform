package com.company.finance_api.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** EurobondQuote — JPA domain entity (eurobond quote). */
@Entity
@Table(name = "eurobond_quotes")
public class EurobondQuote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "isin", nullable = false, length = 12)
  private String isin;

  @Column(name = "clean_price", precision = 14, scale = 6)
  private BigDecimal cleanPrice;

  @Column(name = "bid_price", precision = 14, scale = 6)
  private BigDecimal bidPrice;

  @Column(name = "ask_price", precision = 14, scale = 6)
  private BigDecimal askPrice;

  @Column(name = "yield_to_maturity_percent", precision = 14, scale = 6)
  private BigDecimal yieldToMaturityPercent;

  @Column(name = "daily_change_percent", precision = 14, scale = 6)
  private BigDecimal dailyChangePercent;

  @Column(name = "quote_time", nullable = false)
  private Instant quoteTime;

  @Column(name = "source_provider", nullable = false, length = 64)
  private String sourceProvider;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EurobondQuote() {}

  public static EurobondQuote newSnapshot(
      String isin,
      BigDecimal cleanPrice,
      BigDecimal bidPrice,
      BigDecimal askPrice,
      BigDecimal yieldToMaturityPercent,
      BigDecimal dailyChangePercent,
      Instant quoteTime,
      String sourceProvider) {
    EurobondQuote q = new EurobondQuote();
    q.isin = isin;
    q.cleanPrice = cleanPrice != null ? cleanPrice.setScale(6, RoundingMode.HALF_UP) : null;
    q.bidPrice = bidPrice != null ? bidPrice.setScale(6, RoundingMode.HALF_UP) : null;
    q.askPrice = askPrice != null ? askPrice.setScale(6, RoundingMode.HALF_UP) : null;
    q.yieldToMaturityPercent =
        yieldToMaturityPercent != null
            ? yieldToMaturityPercent.setScale(6, RoundingMode.HALF_UP)
            : null;
    q.dailyChangePercent =
        dailyChangePercent != null ? dailyChangePercent.setScale(6, RoundingMode.HALF_UP) : null;
    q.quoteTime = quoteTime;
    q.sourceProvider = sourceProvider;
    q.createdAt = Instant.now();
    return q;
  }

  public Long getId() {
    return id;
  }

  public String getIsin() {
    return isin;
  }

  public BigDecimal getCleanPrice() {
    return cleanPrice;
  }

  public BigDecimal getBidPrice() {
    return bidPrice;
  }

  public BigDecimal getAskPrice() {
    return askPrice;
  }

  public BigDecimal getYieldToMaturityPercent() {
    return yieldToMaturityPercent;
  }

  public BigDecimal getDailyChangePercent() {
    return dailyChangePercent;
  }

  public Instant getQuoteTime() {
    return quoteTime;
  }

  public String getSourceProvider() {
    return sourceProvider;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
