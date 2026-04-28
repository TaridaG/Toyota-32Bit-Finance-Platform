package com.company.marketdataservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FundDto(
        String fundCode,
        BigDecimal nav,
        String source,
        Instant timestamp
) {
}
