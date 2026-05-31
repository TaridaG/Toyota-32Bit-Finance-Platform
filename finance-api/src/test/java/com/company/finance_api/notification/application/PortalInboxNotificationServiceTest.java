package com.company.finance_api.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.notification.domain.enums.NotificationType;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalInboxNotificationServiceTest {

  @Mock AlarmHistoryRepository alarmHistoryRepository;

  @InjectMocks PortalInboxNotificationService service;

  @Test
  void deliver_persistsSystemNotification() {
    User user = new User("user@example.com", "portal-user");
    setUserId(user, UUID.randomUUID());

    service.deliver(user, NotificationType.ADMIN_MESSAGE, "Title", "Body");

    verify(alarmHistoryRepository).save(any(AlarmHistory.class));
  }

  @Test
  void deliver_noOpWhenUserNull() {
    service.deliver(null, NotificationType.ADMIN_MESSAGE, "Title", "Body");

    verify(alarmHistoryRepository, never()).save(any());
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
