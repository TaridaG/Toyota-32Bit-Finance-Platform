package com.company.finance_api.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceUpdatedEvent(
        String eventId,
        String instrumentSymbol,
        BigDecimal price,
        String priceType,
        String source,
        Instant occurredAt
) {}
