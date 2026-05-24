package com.company.finance_api.admin.application;

import com.company.finance_api.domain.User;
import com.company.finance_api.profile.avatar.ProfileAvatarStorage;
import com.company.finance_api.registration.EmailVerificationCodeRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Portal kullanıcısını Keycloak ve finance veritabanından kalıcı siler (FK cascade). */
@Service
public class UserDeletionProcessor {

  private final UserRepository userRepository;
  private final KeycloakRealmAdminClient keycloakRealmAdminClient;
  private final EmailVerificationCodeRepository emailVerificationCodeRepository;
  private final ProfileAvatarStorage profileAvatarStorage;
  private final AdminPortalRosterCounterService adminPortalRosterCounterService;

  public UserDeletionProcessor(
      UserRepository userRepository,
      KeycloakRealmAdminClient keycloakRealmAdminClient,
      EmailVerificationCodeRepository emailVerificationCodeRepository,
      ProfileAvatarStorage profileAvatarStorage,
      AdminPortalRosterCounterService adminPortalRosterCounterService) {
    this.userRepository = userRepository;
    this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    this.emailVerificationCodeRepository = emailVerificationCodeRepository;
    this.profileAvatarStorage = profileAvatarStorage;
    this.adminPortalRosterCounterService = adminPortalRosterCounterService;
  }

  /** Keycloak, avatar ve yerel kullanıcı kaydını kalıcı olarak kaldırır. */
  @Transactional
  public void hardDelete(User user) {
    if (user == null || user.getId() == null) {
      return;
    }
    Optional<String> keycloakUserId =
        keycloakRealmAdminClient.findUserIdForPortalUser(user.getAuthUsername(), user.getEmail());
    keycloakUserId.ifPresent(
        id -> {
          try {
            keycloakRealmAdminClient.logoutAllSessions(id);
          } catch (RuntimeException ignored) {
            // Best-effort before delete.
          }
          keycloakRealmAdminClient.deleteUser(id);
        });

    profileAvatarStorage.delete(user.getId());
    if (user.getEmail() != null) {
      emailVerificationCodeRepository.deleteById(user.getEmail());
    }
    userRepository.delete(user);
    adminPortalRosterCounterService.recordPermanentAccountDeletion();
  }
}
