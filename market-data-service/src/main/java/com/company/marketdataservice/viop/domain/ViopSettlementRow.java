package com.company.marketdataservice.viop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Canonical VIOP daily settlement row.
 */
public record ViopSettlementRow(
        LocalDate tradeDate,
        String contractCode,
        BigDecimal lastPrice,
        BigDecimal changePercent,
        BigDecimal changeAmount,
        BigDecimal volumeTl,
        BigDecimal volumeQty,
        BigDecimal openInterest,
        String sourceFile
) {}

