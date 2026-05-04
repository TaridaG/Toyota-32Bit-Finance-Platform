package com.company.marketdataservice.historical;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalFxPoint(
        String canonicalSymbol,
        Long instrumentId,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        Instant occurredAt,
        String source
) {
}
