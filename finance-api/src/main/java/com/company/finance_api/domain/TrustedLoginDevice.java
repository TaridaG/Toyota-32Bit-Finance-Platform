package com.company.finance_api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** TrustedLoginDevice — JPA domain entity (trusted login device). */
@Entity
@Table(name = "trusted_login_device")
public class TrustedLoginDevice {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  protected TrustedLoginDevice() {}

  public TrustedLoginDevice(UUID id, UUID userId, String tokenHash, Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void touchLastUsed(Instant at) {
    this.lastUsedAt = at;
  }
}
