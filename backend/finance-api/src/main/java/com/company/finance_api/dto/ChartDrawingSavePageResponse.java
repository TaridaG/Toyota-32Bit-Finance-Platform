package com.company.finance_api.dto;

import java.util.List;

public record ChartDrawingSavePageResponse(
        List<ChartDrawingSaveSummaryDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
