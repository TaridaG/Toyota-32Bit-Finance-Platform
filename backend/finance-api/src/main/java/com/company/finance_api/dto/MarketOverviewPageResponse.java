package com.company.finance_api.dto;

import java.util.List;

public record MarketOverviewPageResponse(
        List<MarketOverviewItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
