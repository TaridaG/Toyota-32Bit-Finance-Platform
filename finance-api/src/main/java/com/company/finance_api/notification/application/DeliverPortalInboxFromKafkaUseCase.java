package com.company.finance_api.notification.application;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.notification.infrastructure.kafka.messaging.PortalInboxDeliverMessage;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Kafka {@code notification.portal.inbox} mesajını portal bildirim kutusuna yazar. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverPortalInboxFromKafkaUseCase {

  private final UserRepository userRepository;
  private final AlarmHistoryRepository alarmHistoryRepository;

  @Transactional
  public void handle(PortalInboxDeliverMessage message) {
    if (message == null || message.getUserId() == null) {
      return;
    }
    UUID userId = message.getUserId();
    Optional<User> userOpt = userRepository.findById(userId);
    if (userOpt.isEmpty()) {
      log.warn("PORTAL_INBOX_SKIPPED userId={} reason=user_not_found", userId);
      return;
    }
    User user = userOpt.get();
    if (!user.isActive() || !user.isNotifyWatchlistAlerts()) {
      log.info(
          "PORTAL_INBOX_SKIPPED userId={} reason=prefs active={} watchlistAlerts={}",
          userId,
          user.isActive(),
          user.isNotifyWatchlistAlerts());
      return;
    }
    String symbol =
        StringUtils.hasText(message.getPrimarySymbol()) ? message.getPrimarySymbol().trim() : null;
    String title = message.getTitle() == null ? "" : message.getTitle().trim();
    String body = message.getBody() == null ? "" : message.getBody().trim();
    alarmHistoryRepository.save(AlarmHistory.watchlistDigest(user.getId(), symbol, title, body));
    log.info("PORTAL_INBOX_SAVED userId={} symbol={}", userId, symbol);
  }
}
