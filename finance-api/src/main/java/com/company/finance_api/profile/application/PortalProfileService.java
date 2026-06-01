package com.company.finance_api.profile.application;

import com.company.finance_api.auth.domain.LoginAttemptContext;
import com.company.finance_api.auth.application.LoginSecurityNotificationService;
import com.company.finance_api.auth.application.PortalTrustedDeviceService;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.http.dto.PortalChangePasswordRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalChangeUsernameRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalConfirmEmailChangeRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalDeleteAccountRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalEmailChangeRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalForgotPasswordResetRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalProfileResponse;
import com.company.finance_api.profile.infrastructure.http.dto.PortalUpdateNotificationsRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalUpdatePhoneRequest;
import com.company.finance_api.profile.infrastructure.http.dto.PortalUpdatePreferencesRequest;
import com.company.finance_api.auth.infrastructure.http.dto.PublicEmailAvailabilityResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicLoginResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.shared.messaging.event.UserDeletionRequestedEvent;
import com.company.finance_api.profile.avatar.ProfileAvatarImageProcessor;
import com.company.finance_api.profile.avatar.ProfileAvatarStorage;
import com.company.finance_api.registration.application.PortalRegistrationService;
import com.company.finance_api.registration.application.RegistrationEmailAvailabilityService;
import com.company.finance_api.registration.application.RegistrationEmailVerificationService;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.outbox.application.OutboxService;
import com.company.finance_api.shared.identity.KeycloakDirectGrantClient;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import com.company.finance_api.shared.kafka.KafkaTopics;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Portal profil yönetimi: avatar, şifre, e-posta, tercihler ve hesap silme talebi. */
@Service
public class PortalProfileService {
  private static final String DEFAULT_PREFERRED_LOCALE = "en";
  private static final String DEFAULT_PREFERRED_CURRENCY = "USD";

  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final KeycloakRealmAdminClient keycloakRealmAdminClient;
  private final KeycloakDirectGrantClient keycloakDirectGrantClient;
  private final ProfileAvatarStorage profileAvatarStorage;
  private final ProfileAvatarImageProcessor profileAvatarImageProcessor;
  private final OutboxService outboxService;
  private final RegistrationEmailVerificationService registrationEmailVerificationService;
  private final PortalRegistrationService portalRegistrationService;
  private final LoginSecurityNotificationService loginSecurityNotificationService;
  private final RegistrationEmailAvailabilityService registrationEmailAvailabilityService;
  private final PortalTrustedDeviceService portalTrustedDeviceService;

  public PortalProfileService(
      CurrentUserResolver currentUserResolver,
      UserRepository userRepository,
      KeycloakRealmAdminClient keycloakRealmAdminClient,
      KeycloakDirectGrantClient keycloakDirectGrantClient,
      ProfileAvatarStorage profileAvatarStorage,
      ProfileAvatarImageProcessor profileAvatarImageProcessor,
      OutboxService outboxService,
      RegistrationEmailVerificationService registrationEmailVerificationService,
      PortalRegistrationService portalRegistrationService,
      LoginSecurityNotificationService loginSecurityNotificationService,
      RegistrationEmailAvailabilityService registrationEmailAvailabilityService,
      PortalTrustedDeviceService portalTrustedDeviceService) {
    this.currentUserResolver = currentUserResolver;
    this.userRepository = userRepository;
    this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    this.keycloakDirectGrantClient = keycloakDirectGrantClient;
    this.profileAvatarStorage = profileAvatarStorage;
    this.profileAvatarImageProcessor = profileAvatarImageProcessor;
    this.outboxService = outboxService;
    this.registrationEmailVerificationService = registrationEmailVerificationService;
    this.portalRegistrationService = portalRegistrationService;
    this.loginSecurityNotificationService = loginSecurityNotificationService;
    this.registrationEmailAvailabilityService = registrationEmailAvailabilityService;
    this.portalTrustedDeviceService = portalTrustedDeviceService;
  }

  /** Oturumu kapatır: trusted device kayıtlarını siler ve cookie header döner. */
  public java.util.Optional<String> logoutCurrentSession() {
    User user = loadCurrentUser();
    portalTrustedDeviceService.revokeAllForUser(user.getId());
    return portalTrustedDeviceService.clearTrustedDeviceCookieHeader();
  }

  /** Oturum açmış kullanıcının profil özetini döner. */
  @Transactional(readOnly = true)
  public PortalProfileResponse getProfile() {
    User user = loadCurrentUser();
    return mapProfile(user);
  }

  /** Mevcut kullanıcının avatar JPEG baytlarını okur. */
  @Transactional(readOnly = true)
  public byte[] readAvatarForCurrentUser() {
    User user = loadCurrentUser();
    if (user.getProfileAvatarUpdatedAt() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return profileAvatarStorage
        .load(user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Admin: başka kullanıcının avatar baytlarını okur (portal ile aynı storage). */
  @Transactional(readOnly = true)
  public byte[] readAvatarForAdminByUserId(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (user.getProfileAvatarUpdatedAt() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return profileAvatarStorage
        .load(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /** Yüklenen görseli işleyip avatar olarak kaydeder. */
  @Transactional
  public PortalProfileResponse uploadAvatar(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an image");
    }
    User user = loadCurrentUser();
    byte[] raw;
    try {
      raw = file.getBytes();
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file", e);
    }
    byte[] processed = profileAvatarImageProcessor.toOptimizedJpeg(raw);
    try {
      profileAvatarStorage.save(user.getId(), processed);
    } catch (UncheckedIOException ex) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not store avatar", ex);
    }
    user.setProfileAvatarUpdatedAt(Instant.now());
    userRepository.save(user);
    return mapProfile(user);
  }

  /** Kullanıcı avatarını siler. */
  @Transactional
  public PortalProfileResponse deleteAvatar() {
    User user = loadCurrentUser();
    if (user.getProfileAvatarUpdatedAt() != null) {
      profileAvatarStorage.delete(user.getId());
    }
    user.setProfileAvatarUpdatedAt(null);
    userRepository.save(user);
    return mapProfile(user);
  }

  /** Mevcut şifreyi doğrulayıp Keycloak'ta yeni şifre ayarlar. */
  public void changePassword(PortalChangePasswordRequest request, LoginAttemptContext context) {
    User user = loadCurrentUser();
    String authUsername = authUsername(user);
    verifyCurrentPassword(authUsername, request.getCurrentPassword());
    resetPasswordInKeycloak(authUsername, request.getNewPassword());
    portalTrustedDeviceService.revokeAllForUser(user.getId());
    loginSecurityNotificationService.notifyPasswordChanged(user, context);
  }

  /** Oturum açmış kullanıcıya şifre sıfırlama doğrulama kodu gönderir. */
  public PublicSendVerificationCodeResponse sendPasswordResetCode() {
    User user = loadCurrentUser();
    if (!StringUtils.hasText(user.getEmail())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Account has no email for verification");
    }
    return registrationEmailVerificationService.sendCode(
        user.getEmail(), user.getPreferredLocale());
  }

  /** E-posta kodu ile şifre sıfırlar (giriş yapmış kullanıcı). */
  public void resetPasswordWithEmailVerification(
      PortalForgotPasswordResetRequest request, LoginAttemptContext context) {
    User user = loadCurrentUser();
    if (!StringUtils.hasText(user.getEmail())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Account has no email for verification");
    }
    registrationEmailVerificationService.verifyCodeOrThrow(
        user.getEmail(), request.getVerificationCode());
    resetPasswordInKeycloak(authUsername(user), request.getNewPassword());
    portalTrustedDeviceService.revokeAllForUser(user.getId());
    loginSecurityNotificationService.notifyPasswordChanged(user, context);
  }

  /** Yeni e-posta adresine doğrulama kodu gönderir. */
  public PublicSendVerificationCodeResponse sendEmailChangeCode(PortalEmailChangeRequest request) {
    User user = loadCurrentUser();
    String newEmail = normalizeEmail(request.getNewEmail());
    if (newEmail.equals(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email unchanged");
    }
    registrationEmailAvailabilityService.assertAvailableForProfileChange(newEmail, user.getId());
    return registrationEmailVerificationService.sendCode(
        newEmail, user.getPreferredLocale(), user.getId());
  }

  /** Doğrulama kodu ile e-posta değişikliğini Keycloak ve DB'de tamamlar. */
  @Transactional
  public PortalProfileResponse confirmEmailChange(
      PortalConfirmEmailChangeRequest request, LoginAttemptContext context) {
    User user = loadCurrentUser();
    String newEmail = normalizeEmail(request.getNewEmail());
    if (newEmail.equals(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email unchanged");
    }
    registrationEmailVerificationService.verifyCodeOrThrow(newEmail, request.getVerificationCode());
    registrationEmailAvailabilityService.assertAvailableForProfileChange(newEmail, user.getId());

    String kcId =
        keycloakRealmAdminClient
            .findUserIdByExactUsername(authUsername(user))
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    String oldEmail = user.getEmail();
    try {
      keycloakRealmAdminClient.updateUserEmail(kcId, newEmail);
      user.setEmail(newEmail);
      userRepository.save(user);
      loginSecurityNotificationService.notifyEmailChanged(user, oldEmail, newEmail, context);
    } catch (RuntimeException ex) {
      if (StringUtils.hasText(oldEmail)) {
        try {
          keycloakRealmAdminClient.updateUserEmail(kcId, oldEmail);
        } catch (RuntimeException ignored) {
          // best-effort rollback
        }
      }
      if (ex instanceof ResponseStatusException rse) {
        throw rse;
      }
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email update failed", ex);
    }
    return mapProfile(user);
  }

  /** Portal görünen kullanıcı adı müsaitliğini kontrol eder. */
  @Transactional(readOnly = true)
  public PublicUsernameAvailabilityResponse checkUsernameAvailability(String usernameInput) {
    User user = loadCurrentUser();
    return portalRegistrationService.checkPortalDisplayUsernameAvailability(
        usernameInput, user.getId());
  }

  /** Profil e-posta değişikliği için müsaitlik kontrolü. */
  @Transactional(readOnly = true)
  public PublicEmailAvailabilityResponse checkEmailAvailability(String emailInput) {
    User user = loadCurrentUser();
    return portalRegistrationService.checkEmailAvailabilityForUser(emailInput, user.getId());
  }

  /** Görünen kullanıcı adını günceller ve yeni token döner. */
  @Transactional
  public PublicLoginResponse changeUsername(
      PortalChangeUsernameRequest request, LoginAttemptContext context) {
    User user = loadCurrentUser();
    String oldUsername = user.getUsername();
    String newUsername = request.getNewUsername().trim().toLowerCase(Locale.ROOT);
    if (oldUsername.equals(newUsername)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kullanıcı adı değişmedi");
    }
    PublicUsernameAvailabilityResponse availability =
        portalRegistrationService.checkPortalDisplayUsernameAvailability(newUsername, user.getId());
    if (!availability.available()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu kullanıcı adı zaten kullanılıyor");
    }
    String authUsername = authUsername(user);
    verifyCurrentPassword(authUsername, request.getCurrentPassword());

    user.changeUsername(newUsername);
    userRepository.save(user);

    loginSecurityNotificationService.notifyUsernameChanged(user, oldUsername, context);
    return keycloakDirectGrantClient.exchangePassword(authUsername, request.getCurrentPassword());
  }

  /** Telefon numarasını günceller. */
  @Transactional
  public PortalProfileResponse updatePhone(PortalUpdatePhoneRequest request) {
    User user = loadCurrentUser();
    if (request.getPhone() == null) {
      return mapProfile(user);
    }
    user.setPhone(normalizePhone(request.getPhone()));
    userRepository.save(user);
    return mapProfile(user);
  }

  /** Bildirim tercihlerini günceller. */
  @Transactional
  public PortalProfileResponse updateNotifications(PortalUpdateNotificationsRequest request) {
    User user = loadCurrentUser();
    user.setNotifySecurityAlerts(Boolean.TRUE.equals(request.getNotifySecurityAlerts()));
    user.setNotifyWatchlistAlerts(Boolean.TRUE.equals(request.getNotifyWatchlistAlerts()));
    user.setNotifyAlarmAlerts(Boolean.TRUE.equals(request.getNotifyAlarmAlerts()));
    userRepository.save(user);
    return mapProfile(user);
  }

  /** Locale ve para birimi tercihlerini günceller. */
  @Transactional
  public PortalProfileResponse updatePreferences(PortalUpdatePreferencesRequest request) {
    User user = loadCurrentUser();
    user.setPreferredLocale(normalizePreferredLocale(request.getPreferredLocale()));
    user.setPreferredCurrency(normalizePreferredCurrency(request.getPreferredCurrency()));
    userRepository.save(user);
    return mapProfile(user);
  }

  /** Hesap silme talebini işaretler ve outbox event kuyruğa alır. */
  @Transactional
  public void requestDeleteAccount(PortalDeleteAccountRequest request) {
    User user = loadCurrentUser();
    if (user.isDeletionRequested()) {
      return;
    }
    verifyCurrentPassword(authUsername(user), request.getCurrentPassword());
    user.markDeletionRequested(Instant.now());
    userRepository.save(user);
    outboxService.enqueue(
        KafkaTopics.INTERNAL_USER_DELETE_REQUESTED,
        user.getId().toString(),
        new UserDeletionRequestedEvent(user.getId(), user.getUsername(), user.getEmail()));
  }

  private PortalProfileResponse mapProfile(User user) {
    return new PortalProfileResponse(
        user.getEmail(),
        user.getUsername(),
        user.getPhone(),
        user.isNotifySecurityAlerts(),
        user.isNotifyWatchlistAlerts(),
        user.isNotifyAlarmAlerts(),
        user.getProfileAvatarUpdatedAt(),
        normalizePreferredLocale(user.getPreferredLocale()),
        normalizePreferredCurrency(user.getPreferredCurrency()),
        user.isTotpEnabled());
  }

  private User loadCurrentUser() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

  private void verifyCurrentPassword(String username, String currentPassword) {
    try {
      keycloakDirectGrantClient.exchangePassword(username, currentPassword);
    } catch (ResponseStatusException ex) {
      if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Current password is incorrect", ex);
      }
      throw ex;
    }
  }

  static String normalizePhone(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (!StringUtils.hasText(trimmed)) {
      return null;
    }
    boolean leadingPlus = trimmed.startsWith("+");
    String digits = trimmed.replaceAll("[^0-9]", "");
    if (!StringUtils.hasText(digits)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone must contain digits");
    }
    String compact = (leadingPlus ? "+" : "") + digits;
    if (compact.length() > 32) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is too long");
    }
    return compact;
  }

  private String normalizePreferredLocale(String raw) {
    if (!StringUtils.hasText(raw)) {
      return DEFAULT_PREFERRED_LOCALE;
    }
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    if (normalized.contains("-")) {
      normalized = normalized.substring(0, normalized.indexOf('-'));
    }
    if (normalized.contains("_")) {
      normalized = normalized.substring(0, normalized.indexOf('_'));
    }
    return switch (normalized) {
      case "en", "tr", "de" -> normalized;
      default ->
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported preferred locale");
    };
  }

  private String normalizePreferredCurrency(String raw) {
    if (!StringUtils.hasText(raw)) {
      return DEFAULT_PREFERRED_CURRENCY;
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "USD", "EUR", "TRY", "GBP", "JPY", "AED" -> normalized;
      default ->
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Unsupported preferred currency");
    };
  }
}
