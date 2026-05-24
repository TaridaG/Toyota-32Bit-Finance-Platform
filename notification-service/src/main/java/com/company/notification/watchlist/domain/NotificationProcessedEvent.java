package com.company.notification.watchlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotent Kafka consumption için işlenmiş watchlist event id'lerini saklar.
 */
@Entity
@Table(name = "notification_processed_events")
public class NotificationProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
