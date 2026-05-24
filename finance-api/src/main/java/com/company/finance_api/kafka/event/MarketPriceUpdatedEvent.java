package com.company.finance_api.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

/** MarketPriceUpdatedEvent — domain/Kafka event payload'u (market price updated event). */
public record MarketPriceUpdatedEvent(
    String eventId,
    String instrumentSymbol,
    BigDecimal price,
    String priceType,
    String source,
    Instant occurredAt,
    Long instrumentId) {}
