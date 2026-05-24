package com.company.finance_api.admin.infrastructure.http.dto;

import java.math.BigDecimal;

/** Admin-only portfolio allocation dökümünde tek satır. */
public record AdminHoldingLineDto(
    String symbol, String instrumentName, BigDecimal marketValue, BigDecimal weightPercent) {}
