package com.company.finance_api.auth;

import com.company.finance_api.bootstrap.config.KeycloakAdminProperties;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PublicLoginMfaRequest;
import com.company.finance_api.dto.PublicLoginRequest;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.dto.PublicRefreshRequest;
import com.company.finance_api.mfa.PortalMfaService;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.identity.KeycloakDirectGrantClient;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import com.company.finance_api.shared.security.PortalAccountGuardService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Portal oturum açma akışını yönetir: Keycloak password grant, MFA challenge ve refresh token. */
@Service
public class PortalLoginService {

  private final KeycloakDirectGrantClient keycloakDirectGrantClient;
  private final KeycloakAdminProperties keycloakAdminProperties;
  private final UserRepository userRepository;
  private final KeycloakRealmAdminClient keycloakRealmAdminClient;
  private final LoginSecurityNotificationService loginSecurityNotificationService;
  private final PortalAccountGuardService portalAccountGuardService;
  private final ObjectProvider<JwtDecoder> jwtDecoder;
  private final PortalMfaService portalMfaService;
  private final PortalTrustedDeviceService portalTrustedDeviceService;

  public PortalLoginService(
      KeycloakDirectGrantClient keycloakDirectGrantClient,
      KeycloakAdminProperties keycloakAdminProperties,
      UserRepository userRepository,
      KeycloakRealmAdminClient keycloakRealmAdminClient,
      LoginSecurityNotificationService loginSecurityNotificationService,
      PortalAccountGuardService portalAccountGuardService,
      ObjectProvider<JwtDecoder> jwtDecoder,
      PortalMfaService portalMfaService,
      PortalTrustedDeviceService portalTrustedDeviceService) {
    this.keycloakDirectGrantClient = keycloakDirectGrantClient;
    this.keycloakAdminProperties = keycloakAdminProperties;
    this.userRepository = userRepository;
    this.keycloakRealmAdminClient = keycloakRealmAdminClient;
    this.loginSecurityNotificationService = loginSecurityNotificationService;
    this.portalAccountGuardService = portalAccountGuardService;
    this.jwtDecoder = jwtDecoder;
    this.portalMfaService = portalMfaService;
    this.portalTrustedDeviceService = portalTrustedDeviceService;
  }

  /**
   * Kullanıcı adı/e-posta ve şifre ile giriş dener; TOTP etkinse MFA challenge veya trusted device
   * bypass döner.
   */
  public LoginCompletionResult login(
      PublicLoginRequest request,
      LoginAttemptContext attemptContext,
      HttpServletRequest httpRequest) {
    KeycloakAdminProperties.Portal portal = keycloakAdminProperties.getPortal();
    if (!StringUtils.hasText(portal.getClientSecret())) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Sign-in is not configured");
    }
    // Match PortalRegistrationService: Keycloak stores email/username lowercased (ROOT).
    String identity = request.getUsername().trim().toLowerCase(Locale.ROOT);
    Optional<User> portalUser = resolvePortalUser(identity);
    LoginAttemptContext ctx =
        attemptContext != null ? attemptContext : new LoginAttemptContext(null, null, null);
    if (portalUser.isPresent() && !portalUser.get().isActive()) {
      loginSecurityNotificationService.notifyLoginFailed(portalUser.get(), ctx);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is pending deletion");
    }
    if (portalUser.isPresent()) {
      portalAccountGuardService.assertNotFrozen(portalUser.get());
    }
    String keycloakUsername = resolveKeycloakUsername(identity, portalUser);

    try {
      PublicLoginResponse response =
          keycloakDirectGrantClient.exchangePassword(keycloakUsername, request.getPassword());
      if (portalUser.isPresent() && portalUser.get().isTotpEnabled()) {
        User user = portalUser.get();
        if (portalTrustedDeviceService.isTrustedForUser(httpRequest, user.getId())) {
          loginSecurityNotificationService.notifyLoginSucceeded(user, ctx);
          return LoginCompletionResult.of(response);
        }
        return LoginCompletionResult.of(portalMfaService.beginLoginChallenge(user, response));
      }
      portalUser.ifPresent(
          user -> loginSecurityNotificationService.notifyLoginSucceeded(user, ctx));
      return LoginCompletionResult.of(response);
    } catch (ResponseStatusException ex) {
      if (ex.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
        throw ex;
      }
      if (portalUser.isPresent()) {
        User user = portalUser.get();
        String canonical = user.getAuthUsername().toLowerCase(Locale.ROOT);
        if (keycloakRealmAdminClient.findUserIdByExactUsername(canonical).isEmpty()) {
          loginSecurityNotificationService.notifyLoginFailed(user, ctx);
          throw new ResponseStatusException(
              HttpStatus.UNAUTHORIZED,
              "Hesabın kayıtlı görünüyor ama oturum açma servisi eşleşmiyor. "
                  + "Kayıt sayfasından aynı e-posta ile tekrar kayıt olmayı deneyin "
                  + "veya destek ile iletişime geçin.");
        }
        loginSecurityNotificationService.notifyLoginFailed(user, ctx);
      }
      throw ex;
    }
  }

  private Optional<User> resolvePortalUser(String normalizedIdentity) {
    Optional<User> byEmail = userRepository.findByEmailIgnoreCase(normalizedIdentity);
    if (byEmail.isPresent()) {
      return byEmail;
    }
    return userRepository.findByUsernameIgnoreCase(normalizedIdentity);
  }

  private String resolveKeycloakUsername(String normalizedIdentity, Optional<User> portalUser) {
    if (portalUser.isPresent()) {
      return portalUser.get().getAuthUsername().toLowerCase(Locale.ROOT);
    }
    if (normalizedIdentity.indexOf('@') >= 0) {
      try {
        return keycloakRealmAdminClient
            .findRealmUsernameByEmail(normalizedIdentity)
            .map(u -> u.toLowerCase(Locale.ROOT))
            .orElse(normalizedIdentity);
      } catch (RuntimeException ignored) {
        return normalizedIdentity;
      }
    }
    return normalizedIdentity;
  }

  /** MFA challenge tamamlar ve gerekirse trusted device cookie üretir. */
  public LoginCompletionResult verifyLoginMfa(
      PublicLoginMfaRequest request, LoginAttemptContext attemptContext) {
    LoginAttemptContext ctx =
        attemptContext != null ? attemptContext : new LoginAttemptContext(null, null, null);
    return portalMfaService.completeLoginChallenge(
        request.challengeId(), request.code(), request.trustDevice(), ctx);
  }

  /** Refresh token ile yeni access token alır; dondurulmuş hesapları reddeder. */
  public PublicLoginResponse refresh(PublicRefreshRequest request) {
    KeycloakAdminProperties.Portal portal = keycloakAdminProperties.getPortal();
    if (!StringUtils.hasText(portal.getClientSecret())) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Sign-in is not configured");
    }
    String submitted = request.getRefreshToken().trim();
    PublicLoginResponse resp = keycloakDirectGrantClient.exchangeRefreshToken(submitted);
    assertAccessTokenNotFrozen(resp.accessToken());
    if (resp.refreshToken() == null && StringUtils.hasText(submitted)) {
      return PublicLoginResponse.complete(
          resp.accessToken(),
          resp.expiresIn() != null ? resp.expiresIn() : 300L,
          resp.tokenType(),
          submitted,
          resp.refreshExpiresIn());
    }
    return resp;
  }

  private void assertAccessTokenNotFrozen(String accessToken) {
    if (!StringUtils.hasText(accessToken)) {
      return;
    }
    JwtDecoder decoder = jwtDecoder.getIfAvailable();
    if (decoder == null) {
      return;
    }
    try {
      Jwt jwt = decoder.decode(accessToken);
      portalAccountGuardService
          .resolvePortalUser(jwt)
          .ifPresent(portalAccountGuardService::assertNotFrozen);
    } catch (RuntimeException ignored) {
      // Decoder unavailable or token not parseable — FrozenAccountFilter still guards API calls.
    }
  }
}
