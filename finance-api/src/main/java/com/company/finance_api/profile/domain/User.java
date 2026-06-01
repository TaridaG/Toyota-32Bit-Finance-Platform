package com.company.finance_api.profile.domain;

import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.util.StringUtils;

/** User — JPA domain entity (user). */
@Entity
@Table(name = "users")
public class User {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  /** Keycloak realm username at registration; used for password grant only. */
  @Column(name = "auth_username", nullable = false, unique = true)
  private String authUsername;

  @Column(length = 32)
  private String phone;

  @Column(nullable = false)
  private boolean notifySecurityAlerts = true;

  @Column(name = "notify_watchlist_alerts", nullable = false)
  private boolean notifyWatchlistAlerts = true;

  @Column(name = "notify_alarm_alerts", nullable = false)
  private boolean notifyAlarmAlerts = true;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "profile_avatar_updated_at")
  private Instant profileAvatarUpdatedAt;

  @Column(name = "preferred_locale", nullable = false, length = 8)
  private String preferredLocale = "en";

  @Column(name = "preferred_currency", nullable = false, length = 8)
  private String preferredCurrency = "USD";

  @Column(name = "deletion_requested_at")
  private Instant deletionRequestedAt;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = true;

  @Column(name = "frozen_at")
  private Instant frozenAt;

  @Column(name = "frozen_reason", length = 500)
  private String frozenReason;

  @Column(name = "totp_secret_encrypted")
  private String totpSecretEncrypted;

  @Column(name = "totp_enabled_at")
  private Instant totpEnabledAt;

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  @JsonIgnore
  private List<ExternalPortfolio> externalPortfolios = new ArrayList<>();

  protected User() {
    // JPA only
  }

  public User(String email, String username) {
    String normalized = username.trim().toLowerCase(Locale.ROOT);
    this.email = email;
    this.username = normalized;
    this.authUsername = normalized;
    this.emailVerified = true;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  public String getUsername() {
    return username;
  }

  public String getAuthUsername() {
    return authUsername;
  }

  public String getPhone() {
    return phone;
  }

  public boolean isNotifySecurityAlerts() {
    return notifySecurityAlerts;
  }

  public boolean isNotifyWatchlistAlerts() {
    return notifyWatchlistAlerts;
  }

  public boolean isNotifyAlarmAlerts() {
    return notifyAlarmAlerts;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getProfileAvatarUpdatedAt() {
    return profileAvatarUpdatedAt;
  }

  public void setProfileAvatarUpdatedAt(Instant profileAvatarUpdatedAt) {
    this.profileAvatarUpdatedAt = profileAvatarUpdatedAt;
  }

  public String getPreferredLocale() {
    return preferredLocale;
  }

  public void setPreferredLocale(String preferredLocale) {
    this.preferredLocale = preferredLocale;
  }

  public String getPreferredCurrency() {
    return preferredCurrency;
  }

  public void setPreferredCurrency(String preferredCurrency) {
    this.preferredCurrency = preferredCurrency;
  }

  public void changeUsername(String newUsername) {
    this.username = newUsername.trim().toLowerCase(Locale.ROOT);
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public void setNotifySecurityAlerts(boolean notifySecurityAlerts) {
    this.notifySecurityAlerts = notifySecurityAlerts;
  }

  public void setNotifyWatchlistAlerts(boolean notifyWatchlistAlerts) {
    this.notifyWatchlistAlerts = notifyWatchlistAlerts;
  }

  public void setNotifyAlarmAlerts(boolean notifyAlarmAlerts) {
    this.notifyAlarmAlerts = notifyAlarmAlerts;
  }

  public Instant getDeletionRequestedAt() {
    return deletionRequestedAt;
  }

  public boolean isDeletionRequested() {
    return deletionRequestedAt != null;
  }

  public void markDeletionRequested(Instant requestedAt) {
    this.active = false;
    this.deletionRequestedAt = requestedAt;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public Instant getFrozenAt() {
    return frozenAt;
  }

  public String getFrozenReason() {
    return frozenReason;
  }

  public boolean isFrozen() {
    return frozenAt != null;
  }

  public void freeze(String reason) {
    this.frozenAt = Instant.now();
    this.frozenReason = reason != null && !reason.isBlank() ? reason.trim() : null;
  }

  public void unfreeze() {
    this.frozenAt = null;
    this.frozenReason = null;
  }

  public boolean isTotpEnabled() {
    return totpEnabledAt != null && StringUtils.hasText(totpSecretEncrypted);
  }

  public String getTotpSecretEncrypted() {
    return totpSecretEncrypted;
  }

  public Instant getTotpEnabledAt() {
    return totpEnabledAt;
  }

  public void setTotpPendingSecretEncrypted(String encryptedSecret) {
    this.totpSecretEncrypted = encryptedSecret;
    this.totpEnabledAt = null;
  }

  public void enableTotp(String encryptedSecret, Instant enabledAt) {
    this.totpSecretEncrypted = encryptedSecret;
    this.totpEnabledAt = enabledAt;
  }

  public void disableTotp() {
    this.totpSecretEncrypted = null;
    this.totpEnabledAt = null;
  }

  public List<ExternalPortfolio> getExternalPortfolios() {
    return externalPortfolios;
  }
}
