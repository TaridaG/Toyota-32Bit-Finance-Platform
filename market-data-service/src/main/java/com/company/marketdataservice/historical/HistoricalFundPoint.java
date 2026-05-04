package com.company.marketdataservice.historical;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalFundPoint(
        String fundCode,
        Long instrumentId,
        BigDecimal nav,
        Instant occurredAt,
        String source
) {
}
