package com.company.finance_api.admin.dto;

import java.util.List;

public record AdminLatencyRunsPageDto(
        List<AdminLatencyRunListItemDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
