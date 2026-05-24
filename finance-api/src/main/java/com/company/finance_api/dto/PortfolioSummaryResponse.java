package com.company.finance_api.dto;

import java.math.BigDecimal;

/** PortfolioSummaryResponse — API transfer nesnesi (DTO/response/request). */
public record PortfolioSummaryResponse(
    BigDecimal totalCost,
    BigDecimal totalValue,
    BigDecimal unrealizedPnl,
    BigDecimal unrealizedPnlPercentage) {}
