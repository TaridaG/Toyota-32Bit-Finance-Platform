package com.company.finance_api.market.infrastructure.http.dto;

import java.util.List;

/** MarketOverviewPageResponse — API transfer nesnesi (DTO/response/request). */
public record MarketOverviewPageResponse(
    List<MarketOverviewItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
