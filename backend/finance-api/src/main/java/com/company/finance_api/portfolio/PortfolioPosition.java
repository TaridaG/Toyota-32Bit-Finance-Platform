package com.company.finance_api.portfolio;

import com.company.finance_api.domain.Instrument;

import java.math.BigDecimal;

public record PortfolioPosition(
        Instrument instrument,
        BigDecimal quantity,
        BigDecimal totalCost,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal unrealizedPnl,
        boolean hasPrice
) {
}
