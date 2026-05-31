package com.company.finance_api.infocards.infrastructure.http.dto;

import java.util.List;

/** Financial literacy catalog sayfalı yanıtı. */
public record LiteracyCatalogPageDto(
    List<InfoCardDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    LiteracyCatalogStatsDto stats) {}
