package com.company.finance_api.admin.dto;

import java.util.List;

public record AdminPortfolioDetailDto(
        long portfolioId,
        String name,
        /** ISO-8601 local date-time of portfolio creation (DB value, no offset). */
        String createdAt,
        String baseCurrency,
        List<AdminHoldingLineDto> holdings
) {}
