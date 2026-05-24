package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Sayfalı latency probe run geçmişi. */
public record AdminLatencyRunsPageDto(
    List<AdminLatencyRunListItemDto> content,
    long totalElements,
    int totalPages,
    int page,
    int size) {}
