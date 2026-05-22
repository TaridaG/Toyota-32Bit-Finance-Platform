package com.company.marketdataservice.provider.yahoo;

import java.math.BigDecimal;
import java.time.Instant;

public record YahooSpotQuote(
        String symbol,
        BigDecimal price,
        Instant timestamp,
        String source,
        BigDecimal volume24h,
        BigDecimal openInterest,
        BigDecimal dayOpen,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        String exchangeName,
        String underlyingSymbol,
        Instant contractExpiry) {
}
