package com.company.finance_api.domain;

import com.company.finance_api.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import java.time.Instant;

/** OutboxEvent — domain/Kafka event payload'u (outbox event). */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "topic", nullable = false, length = 200)
  private String topic;

  @Column(name = "message_key", length = 200)
  private String messageKey;

  @Column(name = "event_type", nullable = false, length = 300)
  private String eventType;

  @Lob
  @Column(name = "payload_json", nullable = false)
  private String payloadJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OutboxStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Lob
  @Column(name = "last_error")
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected OutboxEvent() {}

  public static OutboxEvent newEvent(
      String topic, String messageKey, String eventType, String payloadJson) {
    OutboxEvent e = new OutboxEvent();
    e.topic = topic;
    e.messageKey = messageKey;
    e.eventType = eventType;
    e.payloadJson = payloadJson;
    e.status = OutboxStatus.NEW;
    e.attempts = 0;
    e.createdAt = Instant.now();
    e.nextAttemptAt = Instant.now();
    return e;
  }

  public Long getId() {
    return id;
  }

  public String getTopic() {
    return topic;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void markSent() {
    this.status = OutboxStatus.SENT;
    this.sentAt = Instant.now();
    this.lastError = null;
  }

  public void markRetry(String error, Instant nextAttemptAt) {
    this.status = OutboxStatus.RETRY;
    this.attempts++;
    this.lastError = error;
    this.nextAttemptAt = nextAttemptAt;
  }

  public void markDead(String error) {
    this.status = OutboxStatus.DEAD;
    this.attempts++;
    this.lastError = error;
  }
}
