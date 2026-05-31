package com.company.finance_api.mfa.application;

import com.company.finance_api.auth.domain.LoginAttemptContext;
import com.company.finance_api.auth.domain.LoginCompletionResult;
import com.company.finance_api.auth.application.LoginSecurityNotificationService;
import com.company.finance_api.auth.application.PortalTrustedDeviceService;
import com.company.finance_api.bootstrap.config.MfaProperties;
import com.company.finance_api.domain.LoginMfaChallenge;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PortalMfaSetupResponse;
import com.company.finance_api.dto.PortalMfaStatusResponse;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.repository.LoginMfaChallengeRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.identity.KeycloakDirectGrantClient;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.security.PortalAccountGuardService;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import dev.samstevens.totp.qr.QrData;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** TOTP MFA kurulumu, devre dışı bırakma ve login challenge tamamlama. */
@Service
public class PortalMfaService {

  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final LoginMfaChallengeRepository challengeRepository;
  private final PortalTotpService portalTotpService;
  private final MfaCryptoService mfaCryptoService;
  private final MfaProperties mfaProperties;
  private final KeycloakDirectGrantClient keycloakDirectGrantClient;
  private final PortalAccountGuardService portalAccountGuardService;
  private final LoginSecurityNotificationService loginSecurityNotificationService;
  private final PortalTrustedDeviceService portalTrustedDeviceService;

  public PortalMfaService(
      CurrentUserResolver currentUserResolver,
      UserRepository userRepository,
      LoginMfaChallengeRepository challengeRepository,
      PortalTotpService portalTotpService,
      MfaCryptoService mfaCryptoService,
      MfaProperties mfaProperties,
      KeycloakDirectGrantClient keycloakDirectGrantClient,
      PortalAccountGuardService portalAccountGuardService,
      LoginSecurityNotificationService loginSecurityNotificationService,
      PortalTrustedDeviceService portalTrustedDeviceService) {
    this.currentUserResolver = currentUserResolver;
    this.userRepository = userRepository;
    this.challengeRepository = challengeRepository;
    this.portalTotpService = portalTotpService;
    this.mfaCryptoService = mfaCryptoService;
    this.mfaProperties = mfaProperties;
    this.keycloakDirectGrantClient = keycloakDirectGrantClient;
    this.portalAccountGuardService = portalAccountGuardService;
    this.loginSecurityNotificationService = loginSecurityNotificationService;
    this.portalTrustedDeviceService = portalTrustedDeviceService;
  }

  /** MFA etkinlik durumunu döner. */
  public PortalMfaStatusResponse getStatus() {
    User user = loadCurrentUser();
    return new PortalMfaStatusResponse(user.isTotpEnabled(), user.getTotpEnabledAt());
  }

  /** TOTP secret ve QR kurulum verisini üretir. */
  @Transactional
  public PortalMfaSetupResponse beginSetup() {
    User user = loadCurrentUser();
    portalAccountGuardService.assertNotFrozen(user);
    String secret = portalTotpService.generateSecret();
    user.setTotpPendingSecretEncrypted(mfaCryptoService.encrypt(secret));
    userRepository.save(user);
    String label = user.getEmail() != null ? user.getEmail() : user.getUsername();
    QrData qrData = portalTotpService.buildQrData(label, secret);
    String otpAuthUri = portalTotpService.buildOtpAuthUri(qrData);
    byte[] qrPng = portalTotpService.generateQrPng(qrData);
    String qrBase64 = Base64.getEncoder().encodeToString(qrPng);
    return new PortalMfaSetupResponse(secret, otpAuthUri, qrBase64);
  }

  /** Kurulum kodunu doğrulayıp MFA'yı etkinleştirir. */
  @Transactional
  public PortalMfaStatusResponse confirmSetup(String code) {
    User user = loadCurrentUser();
    portalAccountGuardService.assertNotFrozen(user);
    String secret = requirePendingSecret(user);
    if (!portalTotpService.verifyCode(secret, code)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
    }
    user.enableTotp(mfaCryptoService.encrypt(secret), Instant.now());
    userRepository.save(user);
    return new PortalMfaStatusResponse(true, user.getTotpEnabledAt());
  }

  /** Şifre ve TOTP ile MFA'yı kapatır; trusted device'ları iptal eder. */
  @Transactional
  public PortalMfaStatusResponse disable(String password, String code) {
    User user = loadCurrentUser();
    portalAccountGuardService.assertNotFrozen(user);
    if (!user.isTotpEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Two-factor authentication is not enabled");
    }
    verifyCurrentPassword(user, password);
    String secret = requireEnabledSecret(user);
    if (!portalTotpService.verifyCode(secret, code)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");
    }
    user.disableTotp();
    userRepository.save(user);
    portalTrustedDeviceService.revokeAllForUser(user.getId());
    return new PortalMfaStatusResponse(false, null);
  }

  /** Login sonrası MFA challenge oluşturur; token'ları şifreli saklar. */
  @Transactional
  public PublicLoginResponse beginLoginChallenge(User user, PublicLoginResponse keycloakTokens) {
    purgeExpiredChallenges();
    UUID challengeId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plusSeconds(mfaProperties.getChallengeTtlSeconds());
    LoginMfaChallenge challenge =
        new LoginMfaChallenge(
            challengeId,
            user.getId(),
            mfaCryptoService.encrypt(keycloakTokens.accessToken()),
            keycloakTokens.refreshToken() != null
                ? mfaCryptoService.encrypt(keycloakTokens.refreshToken())
                : null,
            expiresAt);
    challengeRepository.save(challenge);
    return PublicLoginResponse.mfaRequired(challengeId.toString());
  }

  /** MFA kodunu doğrular ve oturumu tamamlar; isteğe bağlı trusted device cookie üretir. */
  @Transactional
  public LoginCompletionResult completeLoginChallenge(
      UUID challengeId, String code, boolean trustDevice, LoginAttemptContext context) {
    purgeExpiredChallenges();
    LoginMfaChallenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Verification session expired"));
    if (challenge.getExpiresAt().isBefore(Instant.now())) {
      challengeRepository.delete(challenge);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification session expired");
    }
    if (challenge.getFailedAttempts() >= mfaProperties.getMaxVerifyAttempts()) {
      challengeRepository.delete(challenge);
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Too many attempts. Please sign in again.");
    }
    User user =
        userRepository
            .findById(challenge.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    portalAccountGuardService.assertNotFrozen(user);
    if (!user.isTotpEnabled()) {
      challengeRepository.delete(challenge);
      return LoginCompletionResult.of(tokensFromChallenge(challenge));
    }
    String secret = requireEnabledSecret(user);
    if (!portalTotpService.verifyCode(secret, code)) {
      challenge.incrementFailedAttempts();
      challengeRepository.save(challenge);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
    }
    PublicLoginResponse tokens = tokensFromChallenge(challenge);
    challengeRepository.delete(challenge);
    loginSecurityNotificationService.notifyLoginSucceeded(user, context);
    if (trustDevice) {
      return portalTrustedDeviceService
          .issueTrustedDeviceCookie(user.getId())
          .map(cookie -> LoginCompletionResult.withCookie(tokens, cookie))
          .orElse(LoginCompletionResult.of(tokens));
    }
    return LoginCompletionResult.of(tokens);
  }

  private PublicLoginResponse tokensFromChallenge(LoginMfaChallenge challenge) {
    String access = mfaCryptoService.decrypt(challenge.getAccessTokenEncrypted());
    String refresh = mfaCryptoService.decrypt(challenge.getRefreshTokenEncrypted());
    if (!StringUtils.hasText(access)) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Sign-in session incomplete");
    }
    return PublicLoginResponse.complete(access, 300L, "Bearer", refresh, null);
  }

  private void purgeExpiredChallenges() {
    challengeRepository.deleteExpiredBefore(Instant.now());
  }

  private User loadCurrentUser() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private String requirePendingSecret(User user) {
    if (!StringUtils.hasText(user.getTotpSecretEncrypted()) || user.isTotpEnabled()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start setup before confirming");
    }
    return mfaCryptoService.decrypt(user.getTotpSecretEncrypted());
  }

  private String requireEnabledSecret(User user) {
    if (!user.isTotpEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Two-factor authentication is not enabled");
    }
    return mfaCryptoService.decrypt(user.getTotpSecretEncrypted());
  }

  private void verifyCurrentPassword(User user, String currentPassword) {
    String username = user.getAuthUsername().toLowerCase(Locale.ROOT);
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
}
