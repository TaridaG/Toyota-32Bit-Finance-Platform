package com.company.finance_api.admin.application;

import com.company.finance_api.auth.application.LoginSecurityNotificationService;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.NotificationType;
import com.company.finance_api.notification.application.PortalInboxNotificationService;
import com.company.finance_api.registration.BlockedRegistrationEmailService;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin kullanıcı dizininden mesaj gönderme, dondurma, çözme ve kalıcı silme işlemlerini yürütür.
 */
@Service
public class AdminUserAccountService {

  private final UserRepository userRepository;
  private final KeycloakRealmAdminClient keycloakRealmAdminClient;
  private final PortalInboxNotificationService portalInboxNotificationService;
  private final LoginSecurityNotificationService loginSecurityNotificationService;
  private final UserDeletionProcessor userDeletionProcessor;
  private final BlockedRegistrationEmailService blockedRegistrationEmailService;

  public AdminUserAccountService(
      UserRepository userRepository,
      KeycloakRealmAdminClient keycloakRealmAdminClient,
      PortalInboxNotificationService portalInboxNotificationService,
      LoginSecurityNotificationService loginSecurityNotificationService,
      UserDeletionProcessor userDeletionProcessor,
      BlockedRegistrationEmailService blockedRegistrationEmailService) {
    this.userRepository = userRepository;
    this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    this.portalInboxNotificationService = portalInboxNotificationService;
    this.loginSecurityNotificationService = loginSecurityNotificationService;
    this.userDeletionProcessor = userDeletionProcessor;
    this.blockedRegistrationEmailService = blockedRegistrationEmailService;
  }

  /** Kullanıcıya admin mesajını inbox ve e-posta ile iletir. */
  @Transactional
  public void sendMessage(UUID userId, String message) {
    User user = requireActiveRosterUser(userId);
    String trimmed = message.trim();
    if (!StringUtils.hasText(trimmed)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
    }
    String title = inboxTitle(user, "adminMessage");
    portalInboxNotificationService.deliver(user, NotificationType.ADMIN_MESSAGE, title, trimmed);
    loginSecurityNotificationService.notifyAdminMessage(user, trimmed);
  }

  /** Hesabı dondurur, Keycloak oturumlarını iptal eder ve bildirim gönderir. */
  @Transactional
  public void freeze(UUID userId, String reason) {
    User user = requireActiveRosterUser(userId);
    if (user.isFrozen()) {
      return;
    }
    String trimmedReason = StringUtils.hasText(reason) ? reason.trim() : null;
    user.freeze(trimmedReason);
    userRepository.save(user);
    revokeKeycloakSessions(user);
    String body = StringUtils.hasText(trimmedReason) ? trimmedReason : null;
    portalInboxNotificationService.deliver(user, NotificationType.ACCOUNT_FROZEN, null, body);
    loginSecurityNotificationService.notifyAccountFrozen(user, trimmedReason);
  }

  /** Hesabı kalıcı siler; isteğe bağlı e-posta engeli uygular. */
  @Transactional
  public void deleteUser(UUID userId, boolean blockEmail) {
    User user = requireActiveRosterUser(userId);
    String email = user.getEmail() != null ? user.getEmail().trim() : null;

    revokeKeycloakSessionsQuietly(user);

    String inboxTitle = inboxTitle(user, "accountDeleted");
    String inboxBody = buildDeletedInboxBody(user, blockEmail);
    portalInboxNotificationService.deliver(
        user, NotificationType.ACCOUNT_DELETED_BY_ADMIN, inboxTitle, inboxBody);
    loginSecurityNotificationService.notifyAccountDeletedByAdmin(user, blockEmail);

    if (blockEmail && StringUtils.hasText(email)) {
      blockedRegistrationEmailService.blockFromDeletedUser(email, user.getId());
    }

    userDeletionProcessor.hardDelete(user);
  }

  /** Dondurmayı kaldırır ve Keycloak hesabını yeniden etkinleştirir. */
  @Transactional
  public void unfreeze(UUID userId) {
    User user = requireActiveRosterUser(userId);
    if (!user.isFrozen()) {
      return;
    }
    user.unfreeze();
    userRepository.save(user);
    enableKeycloakAccount(user);
    portalInboxNotificationService.deliver(user, NotificationType.ACCOUNT_UNFROZEN, null, null);
    loginSecurityNotificationService.notifyAccountUnfrozen(user);
  }

  private User requireActiveRosterUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (user.getDeletionRequestedAt() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "User is pending deletion");
    }
    return user;
  }

  private void revokeKeycloakSessions(User user) {
    String kcId =
        keycloakRealmAdminClient
            .findUserIdForPortalUser(user.getAuthUsername(), user.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Identity account not found"));
    keycloakRealmAdminClient.setUserEnabled(kcId, false);
    keycloakRealmAdminClient.logoutAllSessions(kcId);
  }

  private void revokeKeycloakSessionsQuietly(User user) {
    keycloakRealmAdminClient
        .findUserIdForPortalUser(user.getAuthUsername(), user.getEmail())
        .ifPresent(
            kcId -> {
              try {
                keycloakRealmAdminClient.setUserEnabled(kcId, false);
                keycloakRealmAdminClient.logoutAllSessions(kcId);
              } catch (RuntimeException ignored) {
                // Best-effort; API guard still blocks deleted JWTs.
              }
            });
  }

  private void enableKeycloakAccount(User user) {
    String authUsername = user.getAuthUsername().toLowerCase(Locale.ROOT);
    keycloakRealmAdminClient
        .findUserIdByExactUsername(authUsername)
        .ifPresent(id -> keycloakRealmAdminClient.setUserEnabled(id, true));
  }

  private static String inboxTitle(User user, String kind) {
    return switch (kind) {
      case "adminMessage" -> "Message from support";
      case "accountDeleted" -> "Account removed";
      default -> "Notification";
    };
  }

  private static String buildDeletedInboxBody(User user, boolean blockEmail) {
    if (!blockEmail || user.getEmail() == null || user.getEmail().isBlank()) {
      return "Your Finance Portal account has been permanently removed by an administrator.";
    }
    return "Your Finance Portal account has been permanently removed by an administrator. "
        + "The email address "
        + user.getEmail().trim()
        + " is blocked from registering new accounts.";
  }
}
