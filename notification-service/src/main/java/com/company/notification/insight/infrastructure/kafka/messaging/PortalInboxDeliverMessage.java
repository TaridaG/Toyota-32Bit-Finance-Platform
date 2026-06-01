package com.company.notification.insight.infrastructure.kafka.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code notification.portal.inbox} topic payload — finance-api portal bildirim kutusuna yazım.
 */
public class PortalInboxDeliverMessage {

    private UUID eventId;
    private UUID userId;
    private String notificationType;
    private String title;
    private String body;
    private String primarySymbol;
    private Instant occurredAt;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPrimarySymbol() {
        return primarySymbol;
    }

    public void setPrimarySymbol(String primarySymbol) {
        this.primarySymbol = primarySymbol;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
