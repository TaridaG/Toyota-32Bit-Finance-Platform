package com.company.finance_api.alarm.domain;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "alarm_rules",
    indexes = {
      @Index(name = "idx_alarm_user", columnList = "user_id"),
      @Index(name = "idx_alarm_instrument", columnList = "instrument_id"),
      @Index(name = "idx_alarm_active", columnList = "active")
    })
/** AlarmRule — JPA domain entity (alarm rule). */
public class AlarmRule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Alarmı kuran kullanıcı
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // Alarm hangi enstrüman için
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instrument_id", nullable = false)
  private Instrument instrument;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlarmCondition condition;

  // Hedef fiyat veya yüzde referansı
  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal threshold;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected AlarmRule() {}

  public AlarmRule(
      User user, Instrument instrument, AlarmCondition condition, BigDecimal threshold) {
    this.user = user;
    this.instrument = instrument;
    this.condition = condition;
    this.threshold = threshold;
    this.active = true;
  }

  // GETTERS
  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Instrument getInstrument() {
    return instrument;
  }

  public AlarmCondition getCondition() {
    return condition;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isOwnedBy(UUID userId) {
    return this.user.getId().equals(userId);
  }

  public void deactivate() {
    if (!this.active) {
      throw new IllegalStateException("Alarm is already inactive");
    }
    this.active = false;
  }
}
