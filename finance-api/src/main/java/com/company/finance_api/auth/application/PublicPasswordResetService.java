package com.company.finance_api.auth.application;

import com.company.finance_api.auth.domain.LoginAttemptContext;
import com.company.finance_api.auth.infrastructure.http.dto.PublicPasswordResetRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.registration.application.RegistrationEmailVerificationService;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import com.company.finance_api.shared.security.PortalAccountGuardService;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Kimlik doğrulama gerektirmeyen şifre sıfırlama akışı. */
@Service
public class PublicPasswordResetService {

  private static final String ACCOUNT_NOT_FOUND_MESSAGE =
      "Bu e-posta adresine kayıtlı hesap bulunamadı";
  private static final String NO_EMAIL_MESSAGE =
      "Bu hesapta kayıtlı e-posta yok; şifre sıfırlama kullanılamaz";
  private static final String PENDING_DELETION_MESSAGE =
      "Hesap silme sürecinde. Şifre sıfırlama kullanılamaz";

  private final UserRepository userRepository;
  private final RegistrationEmailVerificationService registrationEmailVerificationService;
  private final KeycloakRealmAdminClient keycloakRealmAdminClient;
  private final PortalAccountGuardService portalAccountGuardService;
  private final PortalTrustedDeviceService portalTrustedDeviceService;
  private final LoginSecurityNotificationService loginSecurityNotificationService;

  public PublicPasswordResetService(
      UserRepository userRepository,
      RegistrationEmailVerificationService registrationEmailVerificationService,
      KeycloakRealmAdminClient keycloakRealmAdminClient,
      PortalAccountGuardService portalAccountGuardService,
      PortalTrustedDeviceService portalTrustedDeviceService,
      LoginSecurityNotificationService loginSecurityNotificationService) {
    this.userRepository = userRepository;
    this.registrationEmailVerificationService = registrationEmailVerificationService;
    this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    this.portalAccountGuardService = portalAccountGuardService;
    this.portalTrustedDeviceService = portalTrustedDeviceService;
    this.loginSecurityNotificationService = loginSecurityNotificationService;
  }

  /** Kayıtlı e-posta adresine şifre sıfırlama doğrulama kodu gönderir. */
  public PublicSendVerificationCodeResponse sendResetCode(String rawEmail, String locale) {
    User user = resolveUserForReset(rawEmail);
    String email = normalizeEmail(rawEmail);
    String localeHint =
        StringUtils.hasText(locale) ? locale : user.getPreferredLocale();
    return registrationEmailVerificationService.sendCodeForPasswordReset(email, localeHint);
  }

  /** E-posta kodu ile şifreyi sıfırlar. */
  public void resetPassword(PublicPasswordResetRequest request, LoginAttemptContext context) {
    User user = resolveUserForReset(request.getEmail());
    String email = normalizeEmail(request.getEmail());
    registrationEmailVerificationService.verifyCodeOrThrow(email, request.getVerificationCode());
    resetPasswordInKeycloak(authUsername(user), request.getNewPassword());
    portalTrustedDeviceService.revokeAllForUser(user.getId());
    loginSecurityNotificationService.notifyPasswordChanged(user, context);
  }

  private User resolveUserForReset(String rawEmail) {
    String email = normalizeEmail(rawEmail);
    User user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND_MESSAGE));
    if (!StringUtils.hasText(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, NO_EMAIL_MESSAGE);
    }
    if (!user.isActive()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, PENDING_DELETION_MESSAGE);
    }
    portalAccountGuardService.assertNotFrozen(user);
    return user;
  }

  private void resetPasswordInKeycloak(String authUsername, String newPassword) {
    String kcId =
        keycloakRealmAdminClient
            .findUserIdByExactUsername(authUsername)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    keycloakRealmAdminClient.resetUserPassword(kcId, newPassword, false);
  }

  private static String authUsername(User user) {
    return user.getAuthUsername().toLowerCase(Locale.ROOT);
  }

  private static String normalizeEmail(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
    }
    return raw.trim().toLowerCase(Locale.ROOT);
  }
}
