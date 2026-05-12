package com.company.finance_api.admin.dto;

public record AdminRecentPortfolioRowDto(
        long id,
        String name,
        String baseCurrency,
        String ownerUsername,
        String createdAt
) {}
