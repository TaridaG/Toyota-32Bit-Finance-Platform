package com.company.finance_api.infocards.infrastructure.http.dto;

import java.util.List;

/** Sayfalı admin info-card listesi. */
public record InfoCardsPageDto(
    List<InfoCardDto> content, int page, int size, long totalElements, int totalPages) {}
