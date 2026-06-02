package com.company.marketdataservice.catalog.infrastructure.http.dto;

import java.time.Instant;

public record IngestCatalogItemDto(
        Long instrumentId,
        String symbol,
        String type,
        String exchange,
        String segment,
        boolean enabled,
        Long totalDays,
        Long recentDays,
        Long recent30Days,
        String lastError,
        Instant lastErrorAt
) {
}

