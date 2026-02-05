package com.company.finance_api.dto;

import java.math.BigDecimal;

public record PortfolioPositionResponse(
        Long instrumentId,
        String instrumentSymbol,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalCost,
        BigDecimal currentValue,
        BigDecimal unrealizedPnl
) {}