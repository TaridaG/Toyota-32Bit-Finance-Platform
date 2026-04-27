package com.company.analytics.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnalyticsInsightEvent(
        UUID eventId,
        Long instrumentId,
        String symbol,
        BigDecimal changePercent,
        String direction,
        Instant occurredAt
) {
}
