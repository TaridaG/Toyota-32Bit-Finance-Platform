package com.company.finance_api.domain;

import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.domain.enums.NotificationType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** AlarmHistory — JPA domain entity (alarm history). */
@Entity
@Table(name = "alarm_history")
@Getter
public class AlarmHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private UUID userId;

  @Column(length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String body;

  private String instrumentSymbol;

  @Enumerated(EnumType.STRING)
  private AlarmCondition condition;

  private BigDecimal threshold;
  private BigDecimal price;
  private Instant triggeredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false)
  private NotificationType notificationType = NotificationType.ALARM;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(nullable = false)
  private boolean deleted = false;

  protected AlarmHistory() {}

  public AlarmHistory(
      UUID userId,
      String instrumentSymbol,
      AlarmCondition condition,
      BigDecimal threshold,
      BigDecimal price) {
    this.userId = userId;
    this.instrumentSymbol = instrumentSymbol;
    this.condition = condition;
    this.threshold = threshold;
    this.price = price;
    this.triggeredAt = Instant.now();
    this.notificationType = NotificationType.ALARM;
  }

  public static AlarmHistory system(
      UUID userId, NotificationType notificationType, String title, String body) {
    AlarmHistory history = new AlarmHistory();
    history.userId = userId;
    history.notificationType = notificationType;
    history.title = title;
    history.body = body;
    history.triggeredAt = Instant.now();
    return history;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public void markRead() {
    if (this.readAt == null) {
      this.readAt = Instant.now();
    }
  }

  public void markDeleted() {
    this.deleted = true;
  }
}
