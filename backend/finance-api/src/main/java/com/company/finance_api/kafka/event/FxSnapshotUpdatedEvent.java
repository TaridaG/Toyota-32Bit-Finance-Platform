package com.company.finance_api.kafka.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

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
}
