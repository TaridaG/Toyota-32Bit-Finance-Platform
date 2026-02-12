package com.company.marketdataservice.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceUpdatedEvent(
        String instrumentSymbol,   // "BTCUSDT"
        BigDecimal price,          // 62500.12
        String priceType,          // "MARKET"
        String source,             // "BINANCE"
        Instant occurredAt
) implements Serializable {

    public static MarketPriceUpdatedEvent of(
            String symbol,
            BigDecimal price,
            String priceType,
            String source
    ) {
        return new MarketPriceUpdatedEvent(symbol, price, priceType, source, Instant.now());
    }
}
