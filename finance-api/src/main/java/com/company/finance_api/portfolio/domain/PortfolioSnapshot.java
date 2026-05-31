package com.company.finance_api.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** PortfolioSnapshot — JPA domain entity (portfolio snapshot). */
@Entity
@Table(name = "portfolio_snapshots")
@Getter
@Setter
public class PortfolioSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // EKLE
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  // EKLE
  @Column(name = "total_cost", nullable = false, precision = 19, scale = 6)
  private BigDecimal totalCost;

  // EKLE
  @Column(name = "total_value", nullable = false, precision = 19, scale = 6)
  private BigDecimal totalValue;

  // EKLE
  @Column(name = "unrealized_pnl", nullable = false, precision = 19, scale = 6)
  private BigDecimal unrealizedPnl;

  // EKLE
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** When set, snapshot is for this external portfolio; legacy rows may be null. */
  @Column(name = "external_portfolio_id")
  private Long externalPortfolioId;
}
