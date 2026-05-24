package com.company.marketdataservice.history.domain;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `geçmiş veri ve backfill` domain katmanı tipi veya port arayüzü.
 */
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
