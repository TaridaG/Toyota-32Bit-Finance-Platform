package com.company.finance_api.auth.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.shared.messaging.event.publisher.LoginSecurityEventPublisher;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginSecurityNotificationServiceTest {

  @Mock LoginSecurityEventPublisher loginSecurityEventPublisher;

  @InjectMocks LoginSecurityNotificationService service;

  @Test
  void notifyAccountFrozen_shouldNotPublish_whenSecurityAlertsDisabled() {
    User user = new User("user@example.com", "testuser");
    user.setNotifySecurityAlerts(false);

    service.notifyAccountFrozen(user, "policy");

    verify(loginSecurityEventPublisher, never()).publish(any());
  }

  @Test
  void notifyAccountFrozen_shouldPublish_whenSecurityAlertsEnabled() {
    User user = new User("user@example.com", "testuser");
    user.setNotifySecurityAlerts(true);

    service.notifyAccountFrozen(user, "policy");

    verify(loginSecurityEventPublisher).publish(any());
  }
}
