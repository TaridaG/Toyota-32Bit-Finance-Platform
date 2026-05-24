package com.company.marketdataservice.shared.observation;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `ortak altyapı` fiyat gözlem modeli.
 */
public record MarketPriceObservation(
        String provider,
        String symbol,
        BigDecimal price,
        Instant timestamp
) {
}
