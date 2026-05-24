package com.company.marketdataservice.fundamentals.infrastructure.http.dto;
import java.math.BigDecimal;

/**
 * `temel veri (fundamentals)` REST API için HTTP DTO.
 */
public record AnnualFinancialStatementDto(
        Integer year,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal operatingCashFlow
) {
}
