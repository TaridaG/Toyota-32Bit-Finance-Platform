package com.company.marketdataservice.dto;

import java.math.BigDecimal;

public record AnnualFinancialStatementDto(
        Integer year,
        BigDecimal revenue,
        BigDecimal netIncome,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal operatingCashFlow
) {
}
