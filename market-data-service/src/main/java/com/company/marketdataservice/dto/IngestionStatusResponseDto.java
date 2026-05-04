package com.company.marketdataservice.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IngestionStatusResponseDto(
        Instant generatedAt,
        Map<String, Long> summary,
        List<IngestionStateItemDto> items
) {
}
