package com.company.finance_api.shared.messaging.event;

import com.company.finance_api.auth.domain.LoginSecurityAlertType;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/** LoginSecurityAlertEvent — domain/Kafka event payload'u (login security alert event). */
@Getter
public class LoginSecurityAlertEvent {

  private final UUID userId;
  private final String userEmail;
  private final String recipientEmail;
  private final String username;
  private final String preferredLocale;
  private final LoginSecurityAlertType alertType;
  private final Instant occurredAt;
  private final String clientIp;
  private final String userAgent;
  private final String previousEmail;
  private final String newEmail;
  private final String previousUsername;
  private final String newUsername;
  private final String adminMessageBody;

  private LoginSecurityAlertEvent(
      UUID userId,
      String userEmail,
      String recipientEmail,
      String username,
      String preferredLocale,
      LoginSecurityAlertType alertType,
      Instant occurredAt,
      String clientIp,
      String userAgent,
      String previousEmail,
      String newEmail,
      String previousUsername,
      String newUsername,
      String adminMessageBody) {
    this.userId = userId;
    this.userEmail = userEmail;
    this.recipientEmail = recipientEmail;
    this.username = username;
    this.preferredLocale = preferredLocale;
    this.alertType = alertType;
    this.occurredAt = occurredAt;
    this.clientIp = clientIp;
    this.userAgent = userAgent;
    this.previousEmail = previousEmail;
    this.newEmail = newEmail;
    this.previousUsername = previousUsername;
    this.newUsername = newUsername;
    this.adminMessageBody = adminMessageBody;
  }

  public String getAdminMessageBody() {
    return adminMessageBody;
  }

  public static LoginSecurityAlertEvent login(
      UUID userId,
      String userEmail,
      String username,
      String preferredLocale,
      LoginSecurityAlertType alertType,
      String clientIp,
      String userAgent) {
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        null,
        username,
        preferredLocale,
        alertType,
        Instant.now(),
        clientIp,
        userAgent,
        null,
        null,
        null,
        null,
        null);
  }

  public static LoginSecurityAlertEvent passwordChanged(
      UUID userId,
      String userEmail,
      String username,
      String preferredLocale,
      String clientIp,
      String userAgent) {
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        null,
        username,
        preferredLocale,
        LoginSecurityAlertType.PASSWORD_CHANGED,
        Instant.now(),
        clientIp,
        userAgent,
        null,
        null,
        null,
        null,
        null);
  }

  public static LoginSecurityAlertEvent usernameChanged(
      UUID userId,
      String userEmail,
      String newUsername,
      String preferredLocale,
      String previousUsername,
      String clientIp,
      String userAgent) {
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        null,
        newUsername,
        preferredLocale,
        LoginSecurityAlertType.USERNAME_CHANGED,
        Instant.now(),
        clientIp,
        userAgent,
        null,
        null,
        previousUsername,
        newUsername,
        null);
  }

  public static LoginSecurityAlertEvent emailChangedOldAccount(
      UUID userId,
      String oldEmail,
      String newEmail,
      String username,
      String preferredLocale,
      String clientIp,
      String userAgent) {
    return new LoginSecurityAlertEvent(
        userId,
        oldEmail,
        oldEmail,
        username,
        preferredLocale,
        LoginSecurityAlertType.EMAIL_CHANGED_OLD_ACCOUNT,
        Instant.now(),
        clientIp,
        userAgent,
        oldEmail,
        newEmail,
        null,
        null,
        null);
  }

  public static LoginSecurityAlertEvent emailChangedNewAccount(
      UUID userId,
      String newEmail,
      String username,
      String preferredLocale,
      String previousEmail,
      String clientIp,
      String userAgent) {
    return new LoginSecurityAlertEvent(
        userId,
        newEmail,
        newEmail,
        username,
        preferredLocale,
        LoginSecurityAlertType.EMAIL_CHANGED_NEW_ACCOUNT,
        Instant.now(),
        clientIp,
        userAgent,
        previousEmail,
        newEmail,
        null,
        null,
        null);
  }

  public static LoginSecurityAlertEvent accountFrozen(
      UUID userId, String userEmail, String username, String preferredLocale, String reason) {
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        null,
        username,
        preferredLocale,
        LoginSecurityAlertType.ACCOUNT_FROZEN,
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        null,
        reason);
  }

  public static LoginSecurityAlertEvent accountUnfrozen(
      UUID userId, String userEmail, String username, String preferredLocale) {
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        null,
        username,
        preferredLocale,
        LoginSecurityAlertType.ACCOUNT_UNFROZEN,
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static LoginSecurityAlertEvent adminMessage(
      UUID userId, String userEmail, String username, String preferredLocale, String messageBody) {
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        null,
        username,
        preferredLocale,
        LoginSecurityAlertType.ADMIN_MESSAGE,
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        null,
        messageBody);
  }

  public static LoginSecurityAlertEvent accountDeletedByAdmin(
      UUID userId,
      String userEmail,
      String username,
      String preferredLocale,
      boolean emailBlocked) {
    String detail =
        emailBlocked ? "EMAIL_BLOCKED:" + (userEmail != null ? userEmail.trim() : "") : null;
    return new LoginSecurityAlertEvent(
        userId,
        userEmail,
        userEmail,
        username,
        preferredLocale,
        LoginSecurityAlertType.ACCOUNT_DELETED_BY_ADMIN,
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        null,
        detail);
  }

  public static LoginSecurityAlertEvent registrationEmailUnblocked(
      String email, String preferredLocale) {
    String normalized = email != null ? email.trim().toLowerCase(java.util.Locale.ROOT) : null;
    return new LoginSecurityAlertEvent(
        null,
        normalized,
        normalized,
        null,
        preferredLocale,
        LoginSecurityAlertType.REGISTRATION_EMAIL_UNBLOCKED,
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
