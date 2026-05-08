package com.company.finance_api.dto;

import java.util.List;

public record TransactionHistoryPageResponse(
        List<TransactionHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

