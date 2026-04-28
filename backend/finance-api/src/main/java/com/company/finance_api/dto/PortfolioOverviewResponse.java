package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioOverviewResponse(
        String currency,
        BigDecimal totalValue,
        BigDecimal totalCost,
        BigDecimal totalPnl,
        BigDecimal totalPnlPercent,
        List<PortfolioOverviewItemResponse> items
) {
}
