package com.company.marketdataservice.history.domain;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `geçmiş veri ve backfill` domain katmanı tipi veya port arayüzü.
 */
public record HistoricalFundPoint(
        String fundCode,
        Long instrumentId,
        BigDecimal nav,
        Instant occurredAt,
        String source
) {
}
