package com.company.finance_api.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.finance_api.bootstrap.config.RegistrationProperties;
import com.company.finance_api.auth.infrastructure.http.dto.PublicEmailAvailabilityResponse;
import com.company.finance_api.auth.infrastructure.http.dto.PublicRegisterRequest;
import com.company.finance_api.profile.application.UserService;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.identity.KeycloakRealmAdminClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PortalRegistrationServiceTest {

  @Mock RegistrationProperties registrationProperties;
  @Mock KeycloakRealmAdminClient keycloakRealmAdminClient;
  @Mock UserRepository userRepository;
  @Mock UserService userService;
  @Mock RegistrationEmailVerificationService registrationEmailVerificationService;
  @Mock RegistrationEmailAvailabilityService registrationEmailAvailabilityService;

  PortalRegistrationService service;

  @BeforeEach
  void setUp() {
    service =
        new PortalRegistrationService(
            registrationProperties,
            keycloakRealmAdminClient,
            userRepository,
            userService,
            registrationEmailVerificationService,
            registrationEmailAvailabilityService);
  }

  @Test
  void register_throwsWhenSelfServiceDisabled() {
    when(registrationProperties.isEnabled()).thenReturn(false);
    PublicRegisterRequest request = new PublicRegisterRequest();
    request.setEmail("user@example.com");
    request.setUsername("user");
    request.setPassword("secret123");
    request.setVerificationCode("123456");

    assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Self-service registration is disabled");
  }

  @Test
  void checkEmailAvailability_delegatesToAvailabilityService() {
    when(registrationEmailAvailabilityService.check("user@example.com", null))
        .thenReturn(new PublicEmailAvailabilityResponse("user@example.com", true, false, List.of()));

    var response = service.checkEmailAvailability("user@example.com");

    assertThat(response.available()).isTrue();
    assertThat(response.normalizedEmail()).isEqualTo("user@example.com");
  }

  @Test
  void checkUsernameAvailability_returnsAvailableWhenFree() {
    when(userRepository.findByUsernameIgnoreCase("trader")).thenReturn(Optional.empty());
    when(keycloakRealmAdminClient.findUserIdByExactUsername("trader")).thenReturn(Optional.empty());

    var response = service.checkUsernameAvailability("Trader");

    assertThat(response.available()).isTrue();
    assertThat(response.normalizedUsername()).isEqualTo("trader");
    assertThat(response.suggestions()).isEmpty();
  }

  @Test
  void checkPortalDisplayUsernameAvailability_allowsSameUser() {
    UUID userId = UUID.randomUUID();
    var existing = new com.company.finance_api.profile.domain.User("me@example.com", "trader");
    setUserId(existing, userId);
    when(userRepository.findByUsernameIgnoreCase("trader")).thenReturn(Optional.of(existing));

    var response = service.checkPortalDisplayUsernameAvailability("Trader", userId);

    assertThat(response.available()).isTrue();
  }

  private static void setUserId(com.company.finance_api.profile.domain.User user, UUID id) {
    try {
      var field = com.company.finance_api.profile.domain.User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
