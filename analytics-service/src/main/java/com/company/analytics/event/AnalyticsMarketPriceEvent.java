package com.company.analytics.event;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsMarketPriceEvent(
        String eventId,
        Long instrumentId,
        String instrumentSymbol,
        BigDecimal price,
        Instant occurredAt
) {
}
