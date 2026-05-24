package com.company.marketdataservice.history.infrastructure.http.dto;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * `geçmiş veri ve backfill` REST API için HTTP DTO.
 */
public record IngestionStatusResponseDto(
        Instant generatedAt,
        Map<String, Long> summary,
        List<IngestionStateItemDto> items
) {
}
