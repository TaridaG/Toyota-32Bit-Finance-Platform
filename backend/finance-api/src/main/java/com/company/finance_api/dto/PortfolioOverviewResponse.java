package com.company.finance_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortfolioOverviewResponse(
        String currency,
        BigDecimal totalValue,
        BigDecimal totalCost,
        BigDecimal totalPnl,
        BigDecimal totalPnlPercent,
        /** totalValue minus prior-day mark (same quantities, last price before start of today UTC). */
        BigDecimal dayOverDayChange,
        List<PortfolioOverviewItemResponse> items
) {
}
