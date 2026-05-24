package com.company.finance_api.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.User;
import com.company.finance_api.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class HeaderCurrentUserResolverTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUserId_should_usePreferredUsernameFromJwt_whenPresent() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserRepository userRepository = mock(UserRepository.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    HeaderCurrentUserResolver resolver =
        new HeaderCurrentUserResolver(request, userRepository, frozenGuard(), meterRegistry, true);

    User user = new User("jwt@example.com", "jwt-user");
    UUID expectedId = UUID.randomUUID();
    setUserId(user, expectedId);
    when(userRepository.findByAuthUsernameIgnoreCase("jwt-user")).thenReturn(Optional.of(user));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwtWithClaims(Map.of("preferred_username", "jwt-user", "sub", "sub-user"))));

    UUID actualId = resolver.getCurrentUserId();

    assertEquals(expectedId, actualId);
  }

  @Test
  void getCurrentUserId_should_resolveByAuthUsername_whenPortalUsernameChanged() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserRepository userRepository = mock(UserRepository.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    HeaderCurrentUserResolver resolver =
        new HeaderCurrentUserResolver(request, userRepository, frozenGuard(), meterRegistry, true);

    User user = new User("me@example.com", "deneyenkado");
    user.changeUsername("amcam");
    UUID expectedId = UUID.randomUUID();
    setUserId(user, expectedId);
    when(userRepository.findByAuthUsernameIgnoreCase("deneyenkado")).thenReturn(Optional.of(user));
    when(userRepository.findByUsernameIgnoreCase("deneyenkado")).thenReturn(Optional.empty());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwtWithClaims(Map.of("preferred_username", "deneyenkado", "sub", "kc-sub"))));

    UUID actualId = resolver.getCurrentUserId();

    assertEquals(expectedId, actualId);
  }

  @Test
  void getCurrentUserId_should_fallbackToHeader_whenNoJwtAndFallbackEnabled() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserRepository userRepository = mock(UserRepository.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    HeaderCurrentUserResolver resolver =
        new HeaderCurrentUserResolver(request, userRepository, frozenGuard(), meterRegistry, true);

    User user = new User("header@example.com", "header-user");
    UUID expectedId = UUID.randomUUID();
    setUserId(user, expectedId);
    when(request.getHeader("X-USERNAME")).thenReturn("header-user");
    when(userRepository.findByAuthUsernameIgnoreCase("header-user")).thenReturn(Optional.of(user));
    when(userRepository.findByUsernameIgnoreCase("header-user")).thenReturn(Optional.of(user));

    UUID actualId = resolver.getCurrentUserId();

    assertEquals(expectedId, actualId);
    assertEquals(
        1.0,
        meterRegistry
            .get("finance_auth_header_fallback_used_total")
            .tag("service", "finance-api")
            .tag("reason", "no_jwt")
            .tag("source", "missing_authorization_header")
            .counter()
            .count());
  }

  @Test
  void getCurrentUserId_should_tagBearerAbsent_whenAuthorizationHeaderNotBearer() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserRepository userRepository = mock(UserRepository.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    HeaderCurrentUserResolver resolver =
        new HeaderCurrentUserResolver(request, userRepository, frozenGuard(), meterRegistry, true);

    User user = new User("header2@example.com", "header-user-2");
    UUID expectedId = UUID.randomUUID();
    setUserId(user, expectedId);
    when(request.getHeader("X-USERNAME")).thenReturn("header-user-2");
    when(request.getHeader("Authorization")).thenReturn("Basic abc");
    when(userRepository.findByAuthUsernameIgnoreCase("header-user-2"))
        .thenReturn(Optional.of(user));
    when(userRepository.findByUsernameIgnoreCase("header-user-2")).thenReturn(Optional.of(user));

    UUID actualId = resolver.getCurrentUserId();

    assertEquals(expectedId, actualId);
    assertEquals(
        1.0,
        meterRegistry
            .get("finance_auth_header_fallback_used_total")
            .tag("service", "finance-api")
            .tag("reason", "no_jwt")
            .tag("source", "bearer_absent")
            .counter()
            .count());
  }

  @Test
  void getCurrentUserId_should_throw_whenNoJwtAndFallbackDisabled() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    UserRepository userRepository = mock(UserRepository.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    HeaderCurrentUserResolver resolver =
        new HeaderCurrentUserResolver(request, userRepository, frozenGuard(), meterRegistry, false);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, resolver::getCurrentUserId);
    assertEquals("Missing authenticated user context", ex.getMessage());
  }

  private static PortalAccountGuardService frozenGuard() {
    PortalAccountGuardService guard = mock(PortalAccountGuardService.class);
    when(guard.isAdmin(any())).thenReturn(false);
    return guard;
  }

  private static Jwt jwtWithClaims(Map<String, Object> claims) {
    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(300),
        Map.of("alg", "none"),
        claims);
  }

  private static void setUserId(User user, UUID id) {
    try {
      var field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
