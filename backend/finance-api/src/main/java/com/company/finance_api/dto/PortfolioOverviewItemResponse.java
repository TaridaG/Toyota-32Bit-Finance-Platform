package com.company.finance_api.dto;

import java.math.BigDecimal;

public record PortfolioOverviewItemResponse(
        Long instrumentId,
        String symbol,
        String name,
        String type,
        BigDecimal quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal value,
        BigDecimal pnl,
        BigDecimal pnlPercent
) {
}
