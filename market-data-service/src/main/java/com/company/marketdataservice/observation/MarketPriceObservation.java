package com.company.marketdataservice.observation;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceObservation(
        String provider,
        String symbol,
        BigDecimal price,
        Instant timestamp
) {
}
