package com.company.finance_api.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.notification.infrastructure.kafka.messaging.PortalInboxDeliverMessage;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliverPortalInboxFromKafkaUseCaseTest {

  @Mock private UserRepository userRepository;
  @Mock private AlarmHistoryRepository alarmHistoryRepository;

  @InjectMocks private DeliverPortalInboxFromKafkaUseCase useCase;

  @Test
  void handle_saves_watchlist_digest_when_prefs_enabled() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "user");
    user.setNotifyWatchlistAlerts(true);

    PortalInboxDeliverMessage message = new PortalInboxDeliverMessage();
    message.setUserId(userId);
    message.setNotificationType("WATCHLIST_DIGEST");
    message.setTitle("Updates");
    message.setBody("AAPL — 5.0%");
    message.setPrimarySymbol("AAPL");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    useCase.handle(message);

    verify(alarmHistoryRepository).save(any(AlarmHistory.class));
  }

  @Test
  void handle_skips_when_watchlist_alerts_disabled() {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "user");
    user.setNotifyWatchlistAlerts(false);

    PortalInboxDeliverMessage message = new PortalInboxDeliverMessage();
    message.setUserId(userId);
    message.setTitle("Updates");

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    useCase.handle(message);

    verify(alarmHistoryRepository, never()).save(any());
  }
}
