package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One daily point of portfolio value and flow-adjusted performance. */
public record PortfolioPerformancePointResponse(
    LocalDate day,
    BigDecimal marketValue,
    BigDecimal netFlow,
    BigDecimal dailyReturnPct,
    BigDecimal twrPct) {}
