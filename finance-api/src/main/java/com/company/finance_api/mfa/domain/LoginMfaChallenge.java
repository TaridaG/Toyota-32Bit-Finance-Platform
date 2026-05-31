package com.company.finance_api.mfa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** LoginMfaChallenge — JPA domain entity (login mfa challenge). */
@Entity
@Table(name = "login_mfa_challenge")
public class LoginMfaChallenge {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "access_token_encrypted", nullable = false)
  private String accessTokenEncrypted;

  @Column(name = "refresh_token_encrypted")
  private String refreshTokenEncrypted;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected LoginMfaChallenge() {}

  public LoginMfaChallenge(
      UUID id,
      UUID userId,
      String accessTokenEncrypted,
      String refreshTokenEncrypted,
      Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.accessTokenEncrypted = accessTokenEncrypted;
    this.refreshTokenEncrypted = refreshTokenEncrypted;
    this.expiresAt = expiresAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getAccessTokenEncrypted() {
    return accessTokenEncrypted;
  }

  public String getRefreshTokenEncrypted() {
    return refreshTokenEncrypted;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public void incrementFailedAttempts() {
    this.failedAttempts++;
  }
}
