package com.company.marketdataservice.spot.infrastructure.http.dto;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `spot fiyat` REST API için HTTP DTO.
 */
public record FundDto(
        String fundCode,
        BigDecimal nav,
        String source,
        Instant timestamp
) {
}
