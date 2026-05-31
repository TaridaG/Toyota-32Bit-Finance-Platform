package com.company.finance_api.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.notification.domain.enums.NotificationType;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PortalNotificationServiceImplTest {

  @Mock AlarmHistoryRepository repository;
  @Mock CurrentUserResolver currentUserResolver;

  @InjectMocks PortalNotificationServiceImpl service;

  @Test
  void getMyPage_returnsUnreadCountAndClampsSize() {
    UUID userId = UUID.randomUUID();
    AlarmHistory row = AlarmHistory.system(userId, NotificationType.ADMIN_MESSAGE, "Title", "Body");
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(repository.findByUserIdAndDeletedFalseOrderByTriggeredAtDesc(any(UUID.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(row)));
    when(repository.countByUserIdAndDeletedFalseAndReadAtIsNull(userId)).thenReturn(1L);

    var page = service.getMyPage(0, 500);

    assertThat(page.size()).isEqualTo(50);
    assertThat(page.unreadCount()).isEqualTo(1L);
    assertThat(page.content()).hasSize(1);
  }

  @Test
  void markRead_persistsReadTimestamp() {
    UUID userId = UUID.randomUUID();
    AlarmHistory row = AlarmHistory.system(userId, NotificationType.ADMIN_MESSAGE, "Title", "Body");
  setHistoryId(row, 42L);
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(repository.findByIdAndUserIdAndDeletedFalse(42L, userId)).thenReturn(Optional.of(row));

    var response = service.markRead(42L);

    assertThat(response.read()).isTrue();
    verify(repository).save(row);
  }

  @Test
  void getById_throwsWhenNotOwned() {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(repository.findByIdAndUserIdAndDeletedFalse(99L, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
  }

  private static void setHistoryId(AlarmHistory history, long id) {
    try {
      var field = AlarmHistory.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(history, id);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
