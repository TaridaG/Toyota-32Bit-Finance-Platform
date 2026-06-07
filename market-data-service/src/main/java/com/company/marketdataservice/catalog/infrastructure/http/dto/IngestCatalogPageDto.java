package com.company.marketdataservice.catalog.infrastructure.http.dto;

import java.util.List;

/**
 * Sayfalı ingest kataloğu API yanıtı.
 */
public record IngestCatalogPageDto(
        List<IngestCatalogItemDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
