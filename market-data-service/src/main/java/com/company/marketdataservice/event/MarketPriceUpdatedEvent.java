package com.company.marketdataservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketPriceUpdatedEvent(
        String eventId,
        String instrumentSymbol,
        BigDecimal price,
        String priceType,
        String source,
        Instant occurredAt,
        Long instrumentId
) implements Serializable {

    public static MarketPriceUpdatedEvent of(
            String symbol,
            BigDecimal price,
            String priceType,
            String source
    ) {
        return of(symbol, price, priceType, source, null);
    }

    public static MarketPriceUpdatedEvent of(
            String symbol,
            BigDecimal price,
            String priceType,
            String source,
            Long instrumentId
    ) {
        return ofAt(symbol, price, priceType, source, instrumentId, Instant.now());
    }

    /**
     * Historical or replayed points: {@code occurredAt} becomes {@code mds_market_price_history.observed_at}.
     */
    public static MarketPriceUpdatedEvent ofAt(
            String symbol,
            BigDecimal price,
            String priceType,
            String source,
            Long instrumentId,
            Instant occurredAt
    ) {
        Instant when = occurredAt != null ? occurredAt : Instant.now();
        return new MarketPriceUpdatedEvent(
                UUID.randomUUID().toString(),
                symbol,
                price,
                priceType,
                source,
                when,
                instrumentId
        );
    }
}
