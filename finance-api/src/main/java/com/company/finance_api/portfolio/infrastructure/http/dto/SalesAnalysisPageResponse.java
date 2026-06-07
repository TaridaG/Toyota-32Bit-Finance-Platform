package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.util.List;

/** SalesAnalysisPageResponse — API transfer nesnesi (sayfalı sales analysis response). */
public record SalesAnalysisPageResponse(
    List<SalesAnalysisRowResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
