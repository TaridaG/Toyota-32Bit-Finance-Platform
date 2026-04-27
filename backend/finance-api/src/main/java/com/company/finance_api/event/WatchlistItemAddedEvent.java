package com.company.finance_api.event;

import java.time.Instant;
import java.util.UUID;

public class WatchlistItemAddedEvent {

    private final UUID eventId;
    private final UUID userId;
    private final Long instrumentId;
    private final String symbol;
    private final Instant occurredAt;

    private WatchlistItemAddedEvent(UUID eventId, UUID userId, Long instrumentId, String symbol, Instant occurredAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.instrumentId = instrumentId;
        this.symbol = symbol;
        this.occurredAt = occurredAt;
    }

    public static WatchlistItemAddedEvent of(UUID userId, Long instrumentId, String symbol) {
        return new WatchlistItemAddedEvent(UUID.randomUUID(), userId, instrumentId, symbol, Instant.now());
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
