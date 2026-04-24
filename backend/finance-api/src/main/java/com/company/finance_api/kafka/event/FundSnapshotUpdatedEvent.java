package com.company.finance_api.kafka.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FundSnapshotUpdatedEvent(
        String eventId,
        String fundCode,
        Long instrumentId,
        BigDecimal nav,
        Instant occurredAt,
        String source
) implements Serializable {
}
