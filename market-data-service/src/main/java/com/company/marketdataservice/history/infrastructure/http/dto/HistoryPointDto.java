package com.company.marketdataservice.history.infrastructure.http.dto;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * `geçmiş veri ve backfill` REST API için HTTP DTO.
 */
public record HistoryPointDto(
        Instant time,
        BigDecimal value
) {
}
