package com.company.marketdataservice.spot.infrastructure.provider.yahoo;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `spot fiyat` infrastructure katmanı adaptörü.
 */
public record YahooSpotQuote(
        String symbol,
        BigDecimal price,
        Instant timestamp,
        String source,
        BigDecimal volume24h,
        BigDecimal openInterest,
        BigDecimal dayOpen,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        String exchangeName,
        String underlyingSymbol,
        Instant contractExpiry) {
}
