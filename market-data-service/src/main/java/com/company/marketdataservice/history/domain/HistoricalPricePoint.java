package com.company.marketdataservice.history.domain;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `geçmiş veri ve backfill` domain katmanı tipi veya port arayüzü.
 */
public record HistoricalPricePoint(
        String symbol,
        BigDecimal price,
        String priceType,
        String source,
        Instant occurredAt,
        Long instrumentId
) {
}
