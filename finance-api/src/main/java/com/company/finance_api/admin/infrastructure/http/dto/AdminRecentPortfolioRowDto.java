package com.company.finance_api.admin.infrastructure.http.dto;

/** Portfolio analytics dashboard'da son oluşturulan portfolio satırı. */
public record AdminRecentPortfolioRowDto(
    long id, String name, String baseCurrency, String ownerUsername, String createdAt) {}
