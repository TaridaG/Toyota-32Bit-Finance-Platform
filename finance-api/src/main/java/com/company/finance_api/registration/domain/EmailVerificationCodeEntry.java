package com.company.finance_api.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Bekleyen e-posta doğrulama kodunun hash ve süre bilgileri. */
@Entity
@Table(name = "email_verification_code")
public class EmailVerificationCodeEntry {
  @Id
  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "code_hash", nullable = false, length = 128)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "resend_available_at", nullable = false)
  private Instant resendAvailableAt;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public void setCodeHash(String codeHash) {
    this.codeHash = codeHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getResendAvailableAt() {
    return resendAvailableAt;
  }

  public void setResendAvailableAt(Instant resendAvailableAt) {
    this.resendAvailableAt = resendAvailableAt;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
