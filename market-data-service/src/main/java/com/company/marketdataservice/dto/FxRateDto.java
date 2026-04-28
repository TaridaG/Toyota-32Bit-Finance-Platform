package com.company.marketdataservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FxRateDto(
        String symbol,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        String source,
        Instant timestamp
) {
}
