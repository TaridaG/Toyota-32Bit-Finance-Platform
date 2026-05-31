package com.company.finance_api.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.registration.domain.EmailAvailabilityException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationEmailAvailabilityServiceTest {

  @Mock UserRepository userRepository;
  @Mock BlockedRegistrationEmailService blockedRegistrationEmailService;

  @InjectMocks RegistrationEmailAvailabilityService service;

  @Test
  void check_returnsUnavailableForMalformedEmail() {
    var result = service.check("not-an-email", null);

    assertThat(result.available()).isFalse();
    assertThat(result.blocked()).isFalse();
    assertThat(result.suggestions()).isEmpty();
  }

  @Test
  void check_returnsBlockedWhenOnBlocklist() {
    when(blockedRegistrationEmailService.isBlocked("blocked@example.com")).thenReturn(true);

    var result = service.check("blocked@example.com", null);

    assertThat(result.available()).isFalse();
    assertThat(result.blocked()).isTrue();
  }

  @Test
  void check_returnsAvailableWhenEmailFree() {
    when(blockedRegistrationEmailService.isBlocked("free@example.com")).thenReturn(false);
    when(userRepository.findByEmailIgnoreCase("free@example.com")).thenReturn(Optional.empty());

    var result = service.check("free@example.com", null);

    assertThat(result.available()).isTrue();
    assertThat(result.blocked()).isFalse();
  }

  @Test
  void assertAvailableForRegistration_throwsWhenEmailInUse() {
    User existing = new User("taken@example.com", "existing");
    when(blockedRegistrationEmailService.isBlocked("taken@example.com")).thenReturn(false);
    when(userRepository.findByEmailIgnoreCase("taken@example.com"))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.assertAvailableForRegistration("taken@example.com"))
        .isInstanceOf(EmailAvailabilityException.class);
  }

  @Test
  void assertAvailableForProfileChange_allowsSameUserEmail() {
    UUID userId = UUID.randomUUID();
    User existing = new User("me@example.com", "me");
    setUserId(existing, userId);

    when(blockedRegistrationEmailService.isBlocked("me@example.com")).thenReturn(false);
    when(userRepository.findByEmailIgnoreCase("me@example.com")).thenReturn(Optional.of(existing));

    service.assertAvailableForProfileChange("me@example.com", userId);
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
