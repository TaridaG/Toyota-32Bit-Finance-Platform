package com.company.finance_api.admin.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.avatar.ProfileAvatarStorage;
import com.company.finance_api.registration.infrastructure.persistence.EmailVerificationCodeRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeletionProcessorTest {

  @Mock UserRepository userRepository;
  @Mock KeycloakRealmAdminClient keycloakRealmAdminClient;
  @Mock EmailVerificationCodeRepository emailVerificationCodeRepository;
  @Mock ProfileAvatarStorage profileAvatarStorage;
  @Mock AdminPortalRosterCounterService adminPortalRosterCounterService;

  @InjectMocks UserDeletionProcessor processor;

  @Test
  void hardDelete_noOpWhenUserNull() {
    processor.hardDelete(null);

    verify(userRepository, never()).delete(any());
    verify(adminPortalRosterCounterService, never()).recordPermanentAccountDeletion();
  }

  @Test
  void hardDelete_removesKeycloakAvatarAndLocalUser() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "portal-user");
    setUserId(user, userId);

    when(keycloakRealmAdminClient.findUserIdForPortalUser("portal-user", "user@example.com"))
        .thenReturn(Optional.of("kc-id"));

    processor.hardDelete(user);

    verify(keycloakRealmAdminClient).logoutAllSessions("kc-id");
    verify(keycloakRealmAdminClient).deleteUser("kc-id");
    verify(profileAvatarStorage).delete(userId);
    verify(emailVerificationCodeRepository).deleteById("user@example.com");
    verify(userRepository).delete(user);
    verify(adminPortalRosterCounterService).recordPermanentAccountDeletion();
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
