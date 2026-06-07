package com.company.marketdataservice.viop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UI alias sembolü ile aktif sözleşme günlük değerini eşleyen domain katmanı tipi.
 */
public record ViopAliasSnapshot(
        String aliasSymbol,
        String contractCode,
        LocalDate tradeDate,
        BigDecimal value
) {}

