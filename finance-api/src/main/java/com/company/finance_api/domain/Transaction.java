package com.company.finance_api.domain;

import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TradeInputMode;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;

/** Transaction — JPA domain entity (transaction). */
@Entity
@Table(name = "transactions")
@Getter
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  @ManyToOne(optional = false)
  private Instrument instrument;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "external_portfolio_id")
  private ExternalPortfolio externalPortfolio;

  @Enumerated(EnumType.STRING)
  private TransactionType type;

  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal price;

  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal totalAmount;

  @Column(nullable = false)
  private Instant createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private PurchaseMode purchaseMode;

  @Column private Instant acquiredAt;

  @Column(precision = 19, scale = 6)
  private BigDecimal unitPrice;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private TradeInputMode inputMode;

  @Column(length = 8)
  private String inputCurrency;

  @Column(precision = 19, scale = 6)
  private BigDecimal inputAmount;

  @Column(precision = 19, scale = 10)
  private BigDecimal fxRateUsed;

  @Column(length = 32)
  private String sourceLabel;

  protected Transaction() {}

  private Transaction(
      User user,
      Instrument instrument,
      ExternalPortfolio externalPortfolio,
      TransactionType type,
      BigDecimal price,
      BigDecimal quantity,
      PurchaseMode purchaseMode,
      Instant acquiredAt,
      BigDecimal unitPrice,
      TradeInputMode inputMode,
      String inputCurrency,
      BigDecimal inputAmount,
      BigDecimal fxRateUsed,
      String sourceLabel) {
    this.user = user;
    this.instrument = instrument;
    this.externalPortfolio = externalPortfolio;
    this.type = type;
    this.price = price;
    this.quantity = quantity;
    this.totalAmount = price.multiply(quantity);
    this.createdAt = Instant.now();
    this.purchaseMode = purchaseMode;
    this.acquiredAt = acquiredAt;
    this.unitPrice = unitPrice;
    this.inputMode = inputMode;
    this.inputCurrency = inputCurrency;
    this.inputAmount = inputAmount;
    this.fxRateUsed = fxRateUsed;
    this.sourceLabel = sourceLabel;
  }

  public static Transaction buy(
      User user, Instrument instrument, BigDecimal price, BigDecimal quantity) {
    return new Transaction(
        user,
        instrument,
        null,
        TransactionType.BUY,
        price,
        quantity,
        PurchaseMode.NOW,
        Instant.now(),
        price,
        TradeInputMode.LOTS,
        "USD",
        price.multiply(quantity),
        BigDecimal.ONE,
        "NOW_BOUGHT");
  }

  public static Transaction buy(
      User user,
      Instrument instrument,
      ExternalPortfolio externalPortfolio,
      BigDecimal price,
      BigDecimal quantity,
      PurchaseMode purchaseMode,
      Instant acquiredAt,
      BigDecimal unitPrice,
      TradeInputMode inputMode,
      String inputCurrency,
      BigDecimal inputAmount,
      BigDecimal fxRateUsed,
      String sourceLabel) {
    return new Transaction(
        user,
        instrument,
        externalPortfolio,
        TransactionType.BUY,
        price,
        quantity,
        purchaseMode,
        acquiredAt,
        unitPrice,
        inputMode,
        inputCurrency,
        inputAmount,
        fxRateUsed,
        sourceLabel);
  }

  public static Transaction sell(
      User user, Instrument instrument, BigDecimal price, BigDecimal quantity) {
    return new Transaction(
        user,
        instrument,
        null,
        TransactionType.SELL,
        price,
        quantity,
        PurchaseMode.NOW,
        Instant.now(),
        price,
        TradeInputMode.LOTS,
        "USD",
        price.multiply(quantity),
        BigDecimal.ONE,
        "NOW_BOUGHT");
  }

  public static Transaction sell(
      User user,
      Instrument instrument,
      ExternalPortfolio externalPortfolio,
      BigDecimal price,
      BigDecimal quantity) {
    return new Transaction(
        user,
        instrument,
        externalPortfolio,
        TransactionType.SELL,
        price,
        quantity,
        PurchaseMode.NOW,
        Instant.now(),
        price,
        TradeInputMode.LOTS,
        "USD",
        price.multiply(quantity),
        BigDecimal.ONE,
        "NOW_BOUGHT");
  }
}
