package com.company.finance_api.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BlockedRegistrationEmail — JPA domain entity (blocked registration email). */
@Entity
@Table(name = "blocked_registration_emails")
public class BlockedRegistrationEmail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 320, unique = true)
  private String email;

  @Column(name = "blocked_at", nullable = false)
  private Instant blockedAt;

  @Column(name = "source_user_id")
  private UUID sourceUserId;

  protected BlockedRegistrationEmail() {}

  public BlockedRegistrationEmail(String email, Instant blockedAt, UUID sourceUserId) {
    this.email = email;
    this.blockedAt = blockedAt;
    this.sourceUserId = sourceUserId;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public Instant getBlockedAt() {
    return blockedAt;
  }

  public UUID getSourceUserId() {
    return sourceUserId;
  }
}
