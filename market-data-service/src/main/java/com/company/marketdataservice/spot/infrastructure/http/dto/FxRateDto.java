package com.company.marketdataservice.spot.infrastructure.http.dto;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `spot fiyat` REST API için HTTP DTO.
 */
public record FxRateDto(
        String symbol,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        String source,
        Instant timestamp
) {
}
