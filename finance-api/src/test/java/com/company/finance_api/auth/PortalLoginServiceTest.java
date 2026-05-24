package com.company.finance_api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.bootstrap.config.KeycloakAdminProperties;
import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PublicLoginRequest;
import com.company.finance_api.dto.PublicLoginResponse;
import com.company.finance_api.dto.PublicRefreshRequest;
import com.company.finance_api.mfa.PortalMfaService;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.identity.KeycloakDirectGrantClient;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import com.company.finance_api.shared.security.PortalAccountGuardService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PortalLoginServiceTest {

  @Mock private KeycloakDirectGrantClient keycloakDirectGrantClient;
  @Mock private UserRepository userRepository;
  @Mock private KeycloakRealmAdminClient keycloakRealmAdminClient;
  @Mock private LoginSecurityNotificationService loginSecurityNotificationService;
  @Mock private PortalAccountGuardService portalAccountGuardService;
  @Mock private ObjectProvider<JwtDecoder> jwtDecoder;
  @Mock private PortalMfaService portalMfaService;
  @Mock private PortalTrustedDeviceService portalTrustedDeviceService;
  @Mock private HttpServletRequest httpServletRequest;

  private KeycloakAdminProperties keycloakAdminProperties;
  private PortalLoginService service;

  @BeforeEach
  void setUp() {
    keycloakAdminProperties = new KeycloakAdminProperties();
    keycloakAdminProperties.getPortal().setClientSecret("portal-client-secret");
    service =
        new PortalLoginService(
            keycloakDirectGrantClient,
            keycloakAdminProperties,
            userRepository,
            keycloakRealmAdminClient,
            loginSecurityNotificationService,
            portalAccountGuardService,
            jwtDecoder,
            portalMfaService,
            portalTrustedDeviceService);
  }

  @Test
  void login_missingClientSecret_throwsServiceUnavailable() {
    keycloakAdminProperties.getPortal().setClientSecret("");
    PublicLoginRequest request = loginRequest("trader@example.com", "secret");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.login(request, null, httpServletRequest));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    verify(keycloakDirectGrantClient, never()).exchangePassword(any(), any());
  }

  @Test
  void login_inactiveUser_throwsUnauthorized() {
    User user = new User("trader@example.com", "trader");
    ReflectionTestUtils.setField(user, "active", false);
    when(userRepository.findByEmailIgnoreCase("trader@example.com")).thenReturn(Optional.of(user));

    PublicLoginRequest request = loginRequest("trader@example.com", "secret");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.login(request, null, httpServletRequest));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Account is pending deletion", ex.getReason());
    verify(loginSecurityNotificationService).notifyLoginFailed(eq(user), any());
    verify(keycloakDirectGrantClient, never()).exchangePassword(any(), any());
  }

  @Test
  void login_success_returnsCompleteTokens() {
    when(userRepository.findByEmailIgnoreCase("trader@example.com")).thenReturn(Optional.empty());
    PublicLoginResponse tokens =
        PublicLoginResponse.complete("access-token", 300L, "Bearer", "refresh-token", 1800L);
    when(keycloakDirectGrantClient.exchangePassword("trader@example.com", "secret")).thenReturn(tokens);

    LoginCompletionResult result =
        service.login(loginRequest("trader@example.com", "secret"), null, httpServletRequest);

    assertThat(result.response().status()).isEqualTo(PublicLoginResponse.STATUS_COMPLETE);
    assertThat(result.response().accessToken()).isEqualTo("access-token");
    assertThat(result.setCookieHeader()).isEmpty();
  }

  @Test
  void refresh_missingClientSecret_throwsServiceUnavailable() {
    keycloakAdminProperties.getPortal().setClientSecret("  ");
    PublicRefreshRequest request = new PublicRefreshRequest();
    request.setRefreshToken("refresh-token");

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.refresh(request));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    verify(keycloakDirectGrantClient, never()).exchangeRefreshToken(any());
  }

  private static PublicLoginRequest loginRequest(String username, String password) {
    PublicLoginRequest request = new PublicLoginRequest();
    request.setUsername(username);
    request.setPassword(password);
    return request;
  }
}
