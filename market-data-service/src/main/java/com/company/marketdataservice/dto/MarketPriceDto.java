package com.company.marketdataservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceDto(
        String symbol,
        BigDecimal price,
        String source,
        Instant timestamp
) {
}
