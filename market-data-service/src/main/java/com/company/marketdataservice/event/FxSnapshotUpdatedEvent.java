package com.company.marketdataservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FxSnapshotUpdatedEvent(
        String eventId,
        String canonicalSymbol,
        Long instrumentId,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal mid,
        Instant occurredAt,
        String source
) implements Serializable {

    public static FxSnapshotUpdatedEvent of(
            String canonicalSymbol,
            Long instrumentId,
            String baseCurrency,
            String quoteCurrency,
            BigDecimal bid,
            BigDecimal ask,
            BigDecimal mid,
            String source
    ) {
        return new FxSnapshotUpdatedEvent(
                UUID.randomUUID().toString(),
                canonicalSymbol,
                instrumentId,
                baseCurrency,
                quoteCurrency,
                bid,
                ask,
                mid,
                Instant.now(),
                source
        );
    }
}
