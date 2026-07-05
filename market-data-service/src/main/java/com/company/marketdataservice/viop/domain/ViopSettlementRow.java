package com.company.marketdataservice.viop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * VIOP günlük uzlaşma (settlement) satırını temsil eden domain katmanı tipi.
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
        String pazar,
        String sourceFile
) {}

