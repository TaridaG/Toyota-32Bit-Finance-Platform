package com.company.finance_api.admin.infrastructure.http.dto;

import java.util.List;

/** Tek external portfolio detayı ve holding satırları. */
public record AdminPortfolioDetailDto(
    long portfolioId,
    String name,
    /** Portfolio oluşturma zamanı ISO-8601 local date-time (DB değeri, offset yok). */
    String createdAt,
    String baseCurrency,
    List<AdminHoldingLineDto> holdings) {}
