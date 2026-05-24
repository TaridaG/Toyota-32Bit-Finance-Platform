package com.company.notification.insight.infrastructure.kafka.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code analytics.insight.simple} fiyat hareketi event'leri için Kafka payload.
 */
public class AnalyticsInsightMessage {

    private UUID eventId;
    private Long instrumentId;
    private String symbol;
    private BigDecimal changePercent;
    private String direction;
    private Instant occurredAt;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
