package com.company.finance_api.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** EurobondInstrument — JPA domain entity (eurobond instrument). */
@Entity
@Table(name = "eurobond_instruments")
public class EurobondInstrument {

  @Id
  @Column(name = "isin", length = 12, nullable = false)
  private String isin;

  @Column(name = "symbol", nullable = false, length = 32)
  private String symbol;

  @Column(name = "name", nullable = false, length = 512)
  private String name;

  @Column(name = "issuer", nullable = false, length = 256)
  private String issuer;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "maturity_date", nullable = false)
  private LocalDate maturityDate;

  @Column(name = "coupon_percent", nullable = false, precision = 10, scale = 4)
  private BigDecimal couponPercent;

  @Column(name = "coupon_frequency", nullable = false, length = 24)
  private String couponFrequency;

  @Column(name = "source_provider", nullable = false, length = 64)
  private String sourceProvider;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected EurobondInstrument() {}

  public String getIsin() {
    return isin;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getName() {
    return name;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  public BigDecimal getCouponPercent() {
    return couponPercent;
  }

  public String getCouponFrequency() {
    return couponFrequency;
  }

  public String getSourceProvider() {
    return sourceProvider;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
