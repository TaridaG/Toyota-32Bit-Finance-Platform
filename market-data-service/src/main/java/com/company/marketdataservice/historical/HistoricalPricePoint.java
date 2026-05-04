package com.company.marketdataservice.historical;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalPricePoint(
        String symbol,
        BigDecimal price,
        String priceType,
        String source,
        Instant occurredAt,
        Long instrumentId
) {
}
