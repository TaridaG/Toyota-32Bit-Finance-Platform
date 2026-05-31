package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.auth.application.LoginSecurityNotificationService;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.notification.domain.enums.NotificationType;
import com.company.finance_api.notification.application.PortalInboxNotificationService;
import com.company.finance_api.registration.application.BlockedRegistrationEmailService;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminUserAccountServiceTest {

  @Mock UserRepository userRepository;
  @Mock KeycloakRealmAdminClient keycloakRealmAdminClient;
  @Mock PortalInboxNotificationService portalInboxNotificationService;
  @Mock LoginSecurityNotificationService loginSecurityNotificationService;
  @Mock UserDeletionProcessor userDeletionProcessor;
  @Mock BlockedRegistrationEmailService blockedRegistrationEmailService;

  @InjectMocks AdminUserAccountService service;

  @Test
  void sendMessage_rejectsBlankMessage() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "portal-user");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.sendMessage(userId, "   "))
        .isInstanceOf(ResponseStatusException.class);

    verify(portalInboxNotificationService, never()).deliver(any(), any(), any(), any());
  }

  @Test
  void sendMessage_deliversInboxAndEmail() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "portal-user");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    service.sendMessage(userId, "Hello from support");

    verify(portalInboxNotificationService)
        .deliver(
            eq(user),
            eq(NotificationType.ADMIN_MESSAGE),
            eq("Message from support"),
            eq("Hello from support"));
    verify(loginSecurityNotificationService).notifyAdminMessage(user, "Hello from support");
  }

  @Test
  void freeze_noOpWhenAlreadyFrozen() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "portal-user");
    user.freeze("prior reason");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    service.freeze(userId, "new reason");

    verify(userRepository, never()).save(user);
    verify(keycloakRealmAdminClient, never()).setUserEnabled(any(), eq(false));
  }

  @Test
  void deleteUser_blocksEmailAndHardDeletes() {
    UUID userId = UUID.randomUUID();
    User user = new User("deleted@example.com", "portal-user");
    setUserId(user, userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(keycloakRealmAdminClient.findUserIdForPortalUser("portal-user", "deleted@example.com"))
        .thenReturn(Optional.of("kc-id"));

    service.deleteUser(userId, true);

    verify(blockedRegistrationEmailService).blockFromDeletedUser("deleted@example.com", userId);
    verify(userDeletionProcessor).hardDelete(user);
  }

  @Test
  void requireActiveRosterUser_rejectsPendingDeletion() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "portal-user");
    user.markDeletionRequested(java.time.Instant.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.sendMessage(userId, "Hi"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void sendMessage_throwsWhenUserMissing() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.sendMessage(userId, "Hi"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private static void setUserId(User user, UUID id) {
    try {
      var field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
