package com.company.finance_api.shared.messaging.event;

import com.company.finance_api.portfolio.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** TransactionExecutedEvent — domain/Kafka event payload'u (transaction executed event). */
@Getter
public class TransactionExecutedEvent {

  private final UUID userId;
  private final Long instrumentId;
  private final String instrumentSymbol;
  private final TransactionType type;
  private final BigDecimal price;
  private final BigDecimal quantity;
  private final BigDecimal totalAmount;
  private final Instant executedAt;

  private TransactionExecutedEvent(
      UUID userId,
      Long instrumentId,
      String instrumentSymbol,
      TransactionType type,
      BigDecimal price,
      BigDecimal quantity,
      BigDecimal totalAmount,
      Instant executedAt) {
    this.userId = userId;
    this.instrumentId = instrumentId;
    this.instrumentSymbol = instrumentSymbol;
    this.type = type;
    this.price = price;
    this.quantity = quantity;
    this.totalAmount = totalAmount;
    this.executedAt = executedAt;
  }

  public static TransactionExecutedEvent of(
      UUID userId,
      Long instrumentId,
      String instrumentSymbol,
      TransactionType type,
      BigDecimal price,
      BigDecimal quantity) {
    return new TransactionExecutedEvent(
        userId,
        instrumentId,
        instrumentSymbol,
        type,
        price,
        quantity,
        price.multiply(quantity),
        Instant.now());
  }
}
