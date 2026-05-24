package com.company.marketdataservice.fund.domain;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `fon (TEFAS NAV)` domain katmanı tipi veya port arayüzü.
 */
public record FundSnapshot(
        Long instrumentId,
        String fundCode,
        BigDecimal nav,
        Instant timestamp,
        String source
) {
}
