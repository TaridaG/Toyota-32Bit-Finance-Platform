package com.company.marketdataservice.fx;

import java.math.BigDecimal;
import java.time.Instant;

public record FxSnapshot(
        String canonicalSymbol,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        Instant timestamp,
        String source
) {
}
