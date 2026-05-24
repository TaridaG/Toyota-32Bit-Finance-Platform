package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Sayfalı market-asset instrument istatistik tablosu. */
public record AdminMarketAssetStatsPageDto(
    List<AdminMarketAssetStatRowDto> content,
    long totalElements,
    int totalPages,
    int page,
    int size) {}
