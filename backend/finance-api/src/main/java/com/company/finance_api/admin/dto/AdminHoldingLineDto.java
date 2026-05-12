package com.company.finance_api.admin.dto;

import java.math.BigDecimal;

/** One line in an admin-only portfolio allocation breakdown. */
public record AdminHoldingLineDto(
        String symbol,
        String instrumentName,
        BigDecimal marketValue,
        BigDecimal weightPercent
) {}
