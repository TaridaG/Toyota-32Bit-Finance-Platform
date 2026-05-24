package com.company.marketdataservice.fx.domain;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `FX spot` domain katmanı tipi veya port arayüzü.
 */
public record FxSnapshot(
        String canonicalSymbol,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        Instant timestamp,
        String source
) {
}
