package com.company.finance_api.registration.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.registration.application.BlockedRegistrationEmailService;
import com.company.finance_api.registration.domain.BlockedRegistrationEmail;
import com.company.finance_api.registration.domain.EmailAvailabilityException;
import com.company.finance_api.registration.infrastructure.persistence.BlockedRegistrationEmailRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BlockedRegistrationEmailServiceTest {

  @Mock BlockedRegistrationEmailRepository repository;

  BlockedRegistrationEmailService service;

  @BeforeEach
  void setUp() {
    service = new BlockedRegistrationEmailService(repository);
  }

  @Test
  void isBlocked_should_normalizeEmailBeforeLookup() {
    when(repository.existsByEmail("blocked@example.com")).thenReturn(true);

    assertTrue(service.isBlocked("  Blocked@Example.com  "));
  }

  @Test
  void isBlocked_should_returnFalseForBlankEmail() {
    assertFalse(service.isBlocked(null));
    assertFalse(service.isBlocked("   "));
    verify(repository, never()).existsByEmail(any());
  }

  @Test
  void ensureNotBlocked_should_throwWhenEmailIsBlocked() {
    when(repository.existsByEmail("blocked@example.com")).thenReturn(true);

    EmailAvailabilityException ex =
        assertThrows(EmailAvailabilityException.class, () -> service.ensureNotBlocked("blocked@example.com"));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    assertEquals(EmailAvailabilityException.CODE_BLOCKED, ex.getErrorCode());
  }

  @Test
  void ensureNotBlocked_should_passWhenEmailIsAllowed() {
    when(repository.existsByEmail("new@example.com")).thenReturn(false);

    assertDoesNotThrow(() -> service.ensureNotBlocked("new@example.com"));
  }

  @Test
  void blockFromDeletedUser_should_persistNormalizedEmail() {
    UUID sourceUserId = UUID.randomUUID();
    when(repository.existsByEmail("deleted@example.com")).thenReturn(false);

    service.blockFromDeletedUser("  Deleted@Example.com  ", sourceUserId);

    ArgumentCaptor<BlockedRegistrationEmail> captor =
        ArgumentCaptor.forClass(BlockedRegistrationEmail.class);
    verify(repository).save(captor.capture());
    BlockedRegistrationEmail saved = captor.getValue();

    assertEquals("deleted@example.com", saved.getEmail());
    assertEquals(sourceUserId, saved.getSourceUserId());
    assertTrue(saved.getBlockedAt() != null);
  }

  @Test
  void blockFromDeletedUser_should_beIdempotentWhenAlreadyBlocked() {
    when(repository.existsByEmail("deleted@example.com")).thenReturn(true);

    service.blockFromDeletedUser("deleted@example.com", UUID.randomUUID());

    verify(repository, never()).save(any(BlockedRegistrationEmail.class));
  }

  @Test
  void blockFromDeletedUser_should_ignoreBlankEmail() {
    service.blockFromDeletedUser("   ", UUID.randomUUID());

    verify(repository, never()).existsByEmail(any());
    verify(repository, never()).save(any(BlockedRegistrationEmail.class));
  }
}
