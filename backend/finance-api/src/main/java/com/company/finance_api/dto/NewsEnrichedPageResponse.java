package com.company.finance_api.dto;

import java.util.List;

public record NewsEnrichedPageResponse(
        List<NewsEnrichedResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
