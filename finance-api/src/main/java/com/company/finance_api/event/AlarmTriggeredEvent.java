package com.company.finance_api.event;

import com.company.finance_api.domain.enums.AlarmCondition;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** AlarmTriggeredEvent — domain/Kafka event payload'u (alarm triggered event). */
@Getter
public class AlarmTriggeredEvent {

  private final Long alarmId;
  private final UUID userId;
  private final String userEmail;
  private final String preferredLocale;
  private final String instrumentSymbol;
  private final AlarmCondition condition;
  private final BigDecimal threshold;
  private final BigDecimal price;
  private final Instant triggeredAt;

  private AlarmTriggeredEvent(
      Long alarmId,
      UUID userId,
      String userEmail,
      String preferredLocale,
      String instrumentSymbol,
      AlarmCondition condition,
      BigDecimal threshold,
      BigDecimal price,
      Instant triggeredAt) {
    this.alarmId = alarmId;
    this.userId = userId;
    this.userEmail = userEmail;
    this.preferredLocale = preferredLocale;
    this.instrumentSymbol = instrumentSymbol;
    this.condition = condition;
    this.threshold = threshold;
    this.price = price;
    this.triggeredAt = triggeredAt;
  }

  public static AlarmTriggeredEvent of(
      Long alarmId,
      UUID userId,
      String userEmail,
      String preferredLocale,
      String instrumentSymbol,
      AlarmCondition condition,
      BigDecimal threshold,
      BigDecimal price) {
    return new AlarmTriggeredEvent(
        alarmId,
        userId,
        userEmail,
        preferredLocale,
        instrumentSymbol,
        condition,
        threshold,
        price,
        Instant.now());
  }
}
