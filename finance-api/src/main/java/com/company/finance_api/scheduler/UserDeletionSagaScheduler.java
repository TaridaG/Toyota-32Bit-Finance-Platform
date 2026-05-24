package com.company.finance_api.scheduler;

import com.company.finance_api.admin.application.UserDeletionProcessor;
import com.company.finance_api.domain.OutboxEvent;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.OutboxStatus;
import com.company.finance_api.event.UserDeletionRequestedEvent;
import com.company.finance_api.repository.OutboxEventRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Kullanıcı silme saga adımlarını ilerletir. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionSagaScheduler {

  private final OutboxEventRepository outboxEventRepository;
  private final UserRepository userRepository;
  private final UserDeletionProcessor userDeletionProcessor;
  private final ObjectMapper objectMapper;

  private final int batchSize = 20;
  private final int maxAttempts = 12;
  private final long backoffSeconds = 10;

  @Scheduled(fixedDelayString = "${saga.user-delete.delay-ms:3000}")
  @Transactional
  public void process() {
    List<OutboxEvent> batch =
        outboxEventRepository.lockBatchByTopic(
            OutboxStatus.NEW,
            OutboxStatus.RETRY,
            Instant.now(),
            KafkaTopics.INTERNAL_USER_DELETE_REQUESTED,
            PageRequest.of(0, batchSize));
    if (batch.isEmpty()) {
      return;
    }
    for (OutboxEvent event : batch) {
      try {
        UserDeletionRequestedEvent payload =
            objectMapper.readValue(event.getPayloadJson(), UserDeletionRequestedEvent.class);
        processSingle(payload);
        event.markSent();
        log.info("USER_DELETE_SAGA_SUCCESS outboxId={} userId={}", event.getId(), payload.userId());
      } catch (Exception ex) {
        String err = safeError(ex);
        if (event.getAttempts() + 1 >= maxAttempts) {
          event.markDead(err);
          log.error("USER_DELETE_SAGA_DEAD outboxId={} error={}", event.getId(), err);
        } else {
          event.markRetry(err, Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
          log.warn(
              "USER_DELETE_SAGA_RETRY outboxId={} attempt={} error={}",
              event.getId(),
              event.getAttempts(),
              err);
        }
      }
    }
  }

  private void processSingle(UserDeletionRequestedEvent payload) {
    if (payload == null || payload.userId() == null) {
      return;
    }
    Optional<User> maybeUser = userRepository.findById(payload.userId());
    if (maybeUser.isEmpty()) {
      // Idempotent success: account already removed in DB.
      return;
    }
    User user = maybeUser.get();
    if (user.isActive() && !user.isDeletionRequested()) {
      throw new IllegalStateException("User deletion requested event received for active user");
    }
    userDeletionProcessor.hardDelete(user);
  }

  private String safeError(Exception ex) {
    String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
    return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
  }
}
