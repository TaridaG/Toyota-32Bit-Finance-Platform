package com.company.finance_api.dto;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        BigDecimal totalCost,
        BigDecimal totalValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPercentage
) {}