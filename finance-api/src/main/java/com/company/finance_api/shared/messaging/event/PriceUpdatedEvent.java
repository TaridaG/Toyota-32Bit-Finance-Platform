package com.company.finance_api.shared.messaging.event;

import com.company.finance_api.pricing.domain.InstrumentPrice;
import java.time.Instant;

/** PriceUpdatedEvent — domain/Kafka event payload'u (price updated event). */
public record PriceUpdatedEvent(InstrumentPrice price, Instant occurredAt) {
  public static PriceUpdatedEvent of(InstrumentPrice price) {
    return new PriceUpdatedEvent(price, Instant.now());
  }
}
