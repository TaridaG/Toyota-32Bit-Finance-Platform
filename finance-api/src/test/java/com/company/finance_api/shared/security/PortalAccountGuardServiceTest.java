package com.company.finance_api.shared.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PortalAccountGuardServiceTest {

  @Mock UserRepository userRepository;

  PortalAccountGuardService guardService;

  @BeforeEach
  void setUp() {
    guardService = new PortalAccountGuardService(userRepository);
  }

  @Test
  void isAdmin_should_returnTrueWhenRoleAdminPresent() {
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwtWithClaims(Map.of("preferred_username", "admin-user")),
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    assertTrue(guardService.isAdmin(authentication));
  }

  @Test
  void isAdmin_should_returnFalseForNullOrNonAdminAuthentication() {
    assertFalse(guardService.isAdmin(null));
    assertFalse(
        guardService.isAdmin(
            new JwtAuthenticationToken(
                jwtWithClaims(Map.of("preferred_username", "portal-user")),
                List.of(new SimpleGrantedAuthority("ROLE_USER")))));
  }

  @Test
  void resolvePortalUser_should_preferAuthUsernameLookup() {
    User user = new User("portal@example.com", "portal-user");
    when(userRepository.findByAuthUsernameIgnoreCase("portal-user")).thenReturn(Optional.of(user));

    Optional<User> resolved =
        guardService.resolvePortalUser(jwtWithClaims(Map.of("preferred_username", "portal-user")));

    assertTrue(resolved.isPresent());
    assertEquals(user, resolved.get());
    verify(userRepository, never()).findByUsernameIgnoreCase("portal-user");
  }

  @Test
  void resolvePortalUser_should_fallbackToSubjectWhenPreferredUsernameMissing() {
    User user = new User("sub@example.com", "sub-user");
    when(userRepository.findByAuthUsernameIgnoreCase("kc-subject")).thenReturn(Optional.empty());
    when(userRepository.findByUsernameIgnoreCase("kc-subject")).thenReturn(Optional.empty());
    when(userRepository.findByEmailIgnoreCase("kc-subject")).thenReturn(Optional.of(user));

    Optional<User> resolved =
        guardService.resolvePortalUser(jwtWithClaims(Map.of("sub", "kc-subject")));

    assertTrue(resolved.isPresent());
    assertEquals(user, resolved.get());
  }

  @Test
  void assertNotFrozen_should_throwForbiddenForFrozenUser() {
    User frozenUser = new User("frozen@example.com", "frozen-user");
    frozenUser.freeze("policy violation");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> guardService.assertNotFrozen(frozenUser));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    assertEquals(PortalAccountGuardService.ACCOUNT_FROZEN_MESSAGE, ex.getReason());
  }

  @Test
  void assertNotFrozen_should_allowActiveUser() {
    User activeUser = new User("active@example.com", "active-user");

    guardService.assertNotFrozen(activeUser);
  }

  @Test
  void assertActivePortalAccount_should_skipFrozenCheckForAdmin() {
    User frozenUser = new User("admin@example.com", "admin-user");
    frozenUser.freeze("policy violation");

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwtWithClaims(Map.of("preferred_username", "admin-user")),
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    guardService.assertActivePortalAccount(authentication);
  }

  private static Jwt jwtWithClaims(Map<String, Object> claims) {
    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(300),
        Map.of("alg", "none"),
        claims);
  }
}
