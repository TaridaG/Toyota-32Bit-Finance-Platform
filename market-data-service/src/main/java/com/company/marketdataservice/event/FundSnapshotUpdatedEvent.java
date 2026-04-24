package com.company.marketdataservice.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FundSnapshotUpdatedEvent(
        String eventId,
        String fundCode,
        Long instrumentId,
        BigDecimal nav,
        Instant occurredAt,
        String source
) implements Serializable {

    public static FundSnapshotUpdatedEvent of(String fundCode, Long instrumentId, BigDecimal nav, String source) {
        return new FundSnapshotUpdatedEvent(
                UUID.randomUUID().toString(),
                fundCode,
                instrumentId,
                nav,
                Instant.now(),
                source
        );
    }
}
