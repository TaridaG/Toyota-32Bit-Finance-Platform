package com.company.finance_api.news.infrastructure.http.dto;

import java.util.List;

/** NewsEnrichedPageResponse — API transfer nesnesi (DTO/response/request). */
public record NewsEnrichedPageResponse(
    List<NewsEnrichedResponse> content, int page, int size, long totalElements, int totalPages) {}
