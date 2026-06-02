package com.company.marketdataservice.viop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alias-to-active-contract resolved daily value (used by UI symbols).
 */
public record ViopAliasSnapshot(
        String aliasSymbol,
        String contractCode,
        LocalDate tradeDate,
        BigDecimal value
) {}

