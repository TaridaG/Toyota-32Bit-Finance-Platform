package com.company.finance_api.auth.application;

import com.company.finance_api.auth.domain.LoginAttemptContext;
import com.company.finance_api.auth.domain.LoginSecurityAlertType;
import com.company.finance_api.domain.User;
import com.company.finance_api.event.LoginSecurityAlertEvent;
import com.company.finance_api.event.publisher.LoginSecurityEventPublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Giriş ve hesap güvenliği olaylarını Kafka üzerinden notification-service'e yayınlar. */
@Service
public class LoginSecurityNotificationService {

  private static final Logger log = LoggerFactory.getLogger(LoginSecurityNotificationService.class);
  private static final Duration FAILED_LOGIN_COOLDOWN = Duration.ofMinutes(10);

  private final LoginSecurityEventPublisher loginSecurityEventPublisher;
  private final Map<String, Instant> lastFailedLoginEmailAt = new ConcurrentHashMap<>();

  public LoginSecurityNotificationService(LoginSecurityEventPublisher loginSecurityEventPublisher) {
    this.loginSecurityEventPublisher = loginSecurityEventPublisher;
  }

  /** Başarılı giriş için güvenlik e-postası tetikler. */
  public void notifyLoginSucceeded(User user, LoginAttemptContext context) {
    publish(
        user,
        LoginSecurityAlertEvent.login(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            resolveLocale(user, context),
            LoginSecurityAlertType.LOGIN_SUCCEEDED,
            contextIp(context),
            contextUa(context)),
        false,
        false);
  }

  /** Başarısız giriş için e-posta gönderir; cooldown ile spam sınırlanır. */
  public void notifyLoginFailed(User user, LoginAttemptContext context) {
    if (isWithinFailedLoginCooldown(user.getId())) {
      log.debug("LOGIN_SECURITY_SKIPPED userId={} reason=failed_login_cooldown", user.getId());
      return;
    }
    publish(
        user,
        LoginSecurityAlertEvent.login(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            resolveLocale(user, context),
            LoginSecurityAlertType.LOGIN_FAILED,
            contextIp(context),
            contextUa(context)),
        true,
        true);
  }

  /** Şifre değişikliği bildirimi yayınlar. */
  public void notifyPasswordChanged(User user, LoginAttemptContext context) {
    publish(
        user,
        LoginSecurityAlertEvent.passwordChanged(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            resolveLocale(user, context),
            contextIp(context),
            contextUa(context)),
        false,
        false);
  }

  /** Kullanıcı adı değişikliği bildirimi yayınlar. */
  public void notifyUsernameChanged(
      User user, String previousUsername, LoginAttemptContext context) {
    publish(
        user,
        LoginSecurityAlertEvent.usernameChanged(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            resolveLocale(user, context),
            previousUsername,
            contextIp(context),
            contextUa(context)),
        false,
        false);
  }

  /** Hesap dondurma bildirimi yayınlar. */
  public void notifyAccountFrozen(User user, String reason) {
    publish(
        user,
        LoginSecurityAlertEvent.accountFrozen(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            user.getPreferredLocale(),
            reason),
        false,
        true);
  }

  /** Hesap çözme (unfreeze) bildirimi yayınlar. */
  public void notifyAccountUnfrozen(User user) {
    publish(
        user,
        LoginSecurityAlertEvent.accountUnfrozen(
            user.getId(), requireEmail(user), user.getUsername(), user.getPreferredLocale()),
        false,
        true);
  }

  /** Admin tarafından gönderilen mesajı güvenlik kanalıyla iletir. */
  public void notifyAdminMessage(User user, String messageBody) {
    publish(
        user,
        LoginSecurityAlertEvent.adminMessage(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            user.getPreferredLocale(),
            messageBody),
        false,
        true);
  }

  /** Admin hesap silme işlemi sonrası bildirim yayınlar. */
  public void notifyAccountDeletedByAdmin(User user, boolean emailBlocked) {
    publish(
        user,
        LoginSecurityAlertEvent.accountDeletedByAdmin(
            user.getId(),
            requireEmail(user),
            user.getUsername(),
            user.getPreferredLocale(),
            emailBlocked),
        false,
        true);
  }

  /**
   * Kayıt engeli kaldırıldığında eski e-posta adresine bildirim gönderir (portal user gerekmez).
   */
  public void notifyRegistrationEmailUnblocked(String email, String preferredLocale) {
    if (!StringUtils.hasText(email)) {
      return;
    }
    String locale = StringUtils.hasText(preferredLocale) ? preferredLocale.trim() : "tr";
    try {
      loginSecurityEventPublisher.publish(
          LoginSecurityAlertEvent.registrationEmailUnblocked(email.trim(), locale));
    } catch (Exception ex) {
      log.error(
          "LOGIN_SECURITY_PUBLISH_FAILED type=REGISTRATION_EMAIL_UNBLOCKED email={}", email, ex);
    }
  }

  /** E-posta değişikliğinde hem eski hem yeni adrese bildirim yayınlar. */
  public void notifyEmailChanged(
      User user, String oldEmail, String newEmail, LoginAttemptContext context) {
    String locale = resolveLocale(user, context);
    String ip = contextIp(context);
    String ua = contextUa(context);
    if (StringUtils.hasText(oldEmail)) {
      publish(
          user,
          LoginSecurityAlertEvent.emailChangedOldAccount(
              user.getId(), oldEmail.trim(), newEmail, user.getUsername(), locale, ip, ua),
          false,
          true);
    }
    if (StringUtils.hasText(newEmail)) {
      publish(
          user,
          LoginSecurityAlertEvent.emailChangedNewAccount(
              user.getId(), newEmail.trim(), user.getUsername(), locale, oldEmail, ip, ua),
          false,
          true);
    }
  }

  private void publish(
      User user,
      LoginSecurityAlertEvent event,
      boolean trackFailedLoginCooldown,
      boolean forceSend) {
    if (user == null) {
      return;
    }
    if (!forceSend && !user.isNotifySecurityAlerts()) {
      return;
    }
    String to =
        StringUtils.hasText(event.getRecipientEmail())
            ? event.getRecipientEmail().trim()
            : event.getUserEmail();
    if (!StringUtils.hasText(to)) {
      log.warn(
          "LOGIN_SECURITY_SKIPPED userId={} type={} reason=no_recipient",
          user.getId(),
          event.getAlertType());
      return;
    }

    try {
      loginSecurityEventPublisher.publish(event);
      if (trackFailedLoginCooldown) {
        lastFailedLoginEmailAt.put(cooldownKey(user.getId()), Instant.now());
      }
    } catch (Exception ex) {
      log.error(
          "LOGIN_SECURITY_PUBLISH_FAILED userId={} type={}",
          user.getId(),
          event.getAlertType(),
          ex);
    }
  }

  private static String requireEmail(User user) {
    return user.getEmail() != null ? user.getEmail().trim() : null;
  }

  private boolean isWithinFailedLoginCooldown(UUID userId) {
    Instant last = lastFailedLoginEmailAt.get(cooldownKey(userId));
    if (last == null) {
      return false;
    }
    return last.plus(FAILED_LOGIN_COOLDOWN).isAfter(Instant.now());
  }

  private static String cooldownKey(UUID userId) {
    return userId.toString();
  }

  private static String resolveLocale(User user, LoginAttemptContext context) {
    if (StringUtils.hasText(user.getPreferredLocale())) {
      return user.getPreferredLocale().trim();
    }
    if (context != null && StringUtils.hasText(context.preferredLocale())) {
      return context.preferredLocale().trim();
    }
    return "en";
  }

  private static String contextIp(LoginAttemptContext context) {
    return context != null ? context.clientIp() : null;
  }

  private static String contextUa(LoginAttemptContext context) {
    return context != null ? context.userAgent() : null;
  }
}
