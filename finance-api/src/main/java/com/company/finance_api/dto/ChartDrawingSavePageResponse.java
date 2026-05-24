package com.company.finance_api.dto;

import java.util.List;

/** ChartDrawingSavePageResponse — API transfer nesnesi (DTO/response/request). */
public record ChartDrawingSavePageResponse(
    List<ChartDrawingSaveSummaryDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
