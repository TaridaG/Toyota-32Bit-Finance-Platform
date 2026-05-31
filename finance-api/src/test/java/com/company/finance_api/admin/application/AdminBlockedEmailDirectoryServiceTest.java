package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.auth.application.LoginSecurityNotificationService;
import com.company.finance_api.registration.domain.BlockedRegistrationEmail;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.registration.infrastructure.persistence.BlockedRegistrationEmailRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminBlockedEmailDirectoryServiceTest {

  @Mock BlockedRegistrationEmailRepository blockedRegistrationEmailRepository;
  @Mock UserRepository userRepository;
  @Mock LoginSecurityNotificationService loginSecurityNotificationService;

  @InjectMocks AdminBlockedEmailDirectoryService service;

  @Test
  void list_clampsPageSizeAndMapsRows() {
    UUID sourceUserId = UUID.randomUUID();
    BlockedRegistrationEmail row =
        new BlockedRegistrationEmail("blocked@example.com", Instant.now(), sourceUserId);
    when(blockedRegistrationEmailRepository.findAllByOrderByBlockedAtDesc(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 50), 1));

    var page = service.list(0, 200);

    assertThat(page.content()).hasSize(1);
    assertThat(page.content().getFirst().email()).isEqualTo("blocked@example.com");
    assertThat(page.size()).isEqualTo(50);
  }

  @Test
  void unblock_deletesRowAndNotifiesWithUserLocale() {
  UUID sourceUserId = UUID.randomUUID();
    BlockedRegistrationEmail row =
        new BlockedRegistrationEmail("blocked@example.com", Instant.now(), sourceUserId);
    User user = new User("blocked@example.com", "blocked");
    user.setPreferredLocale("en");

    when(blockedRegistrationEmailRepository.findById(7L)).thenReturn(Optional.of(row));
    when(userRepository.findById(sourceUserId)).thenReturn(Optional.of(user));

    service.unblock(7L);

    verify(blockedRegistrationEmailRepository).delete(row);
    verify(loginSecurityNotificationService)
        .notifyRegistrationEmailUnblocked("blocked@example.com", "en");
  }

  @Test
  void unblock_throwsWhenRowMissing() {
    when(blockedRegistrationEmailRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.unblock(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
