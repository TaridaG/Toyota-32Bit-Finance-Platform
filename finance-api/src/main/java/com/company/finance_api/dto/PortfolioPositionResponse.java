package com.company.finance_api.dto;

import java.math.BigDecimal;

/** PortfolioPositionResponse — API transfer nesnesi (DTO/response/request). */
public record PortfolioPositionResponse(
    Long instrumentId,
    String instrumentSymbol,
    BigDecimal quantity,
    BigDecimal averagePrice,
    BigDecimal currentPrice,
    BigDecimal totalCost,
    BigDecimal currentValue,
    BigDecimal unrealizedPnl) {}
