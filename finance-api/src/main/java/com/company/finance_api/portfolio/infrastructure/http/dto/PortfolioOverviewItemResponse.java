package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.math.BigDecimal;

/** PortfolioOverviewItemResponse — API transfer nesnesi (DTO/response/request). */
public record PortfolioOverviewItemResponse(
    Long instrumentId,
    String symbol,
    String name,
    String type,
    /** {@link com.company.finance_api.instrument.domain.enums.Exchange} name, or null when not set. */
    String exchange,
    BigDecimal quantity,
    BigDecimal avgBuyPrice,
    BigDecimal currentPrice,
    BigDecimal value,
    /**
     * Mark-to-market value using last price before start of today (UTC) × quantity held at that
     * cut-off; null when unavailable.
     */
    BigDecimal priorDayValue,
    BigDecimal pnl,
    BigDecimal pnlPercent) {}
