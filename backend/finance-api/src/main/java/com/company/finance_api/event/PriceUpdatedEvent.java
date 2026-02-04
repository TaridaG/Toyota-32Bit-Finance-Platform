package com.company.finance_api.event;

import com.company.finance_api.domain.InstrumentPrice;

import java.time.Instant;

public record PriceUpdatedEvent(
        InstrumentPrice price,
        Instant occurredAt
) {
    public static PriceUpdatedEvent of(InstrumentPrice price) {
        return new PriceUpdatedEvent(price, Instant.now());
    }
}