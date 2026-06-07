package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** SalesAnalysisRowResponse — API transfer nesnesi (tek kapanmış satış satırı, realized/hypothetical P/L). */
public record SalesAnalysisRowResponse(
    Long transactionId,
    Long portfolioId,
    String portfolioName,
    Long instrumentId,
    String instrumentSymbol,
    BigDecimal quantity,
    BigDecimal avgCostAtSell,
    BigDecimal sellUnitPrice,
    BigDecimal sellProceeds,
    BigDecimal costBasis,
    BigDecimal realizedPnl,
    BigDecimal realizedPnlPct,
    BigDecimal currentUnitPrice,
    BigDecimal hypotheticalValueNow,
    BigDecimal opportunityDelta,
    String quoteCurrency,
    Instant soldAt) {}
