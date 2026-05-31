package com.company.finance_api.outbox.infrastructure.scheduler;

import com.company.finance_api.outbox.domain.OutboxEvent;
import com.company.finance_api.outbox.domain.enums.OutboxStatus;
import com.company.finance_api.outbox.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Outbox tablosundaki bekleyen event'leri Kafka'ya publish eder. */
@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

  private final OutboxEventRepository outboxEventRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  // tunables (istersen sonra application.yml’den property’e taşırız)
  private final int batchSize = 50;
  private final int maxAttempts = 8;
  private final long backoffSeconds = 5;

  @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:2000}")
  @Transactional
  public void publish() {
    Instant now = Instant.now();

    List<OutboxEvent> batch =
        outboxEventRepository.lockBatch(
            OutboxStatus.NEW, OutboxStatus.RETRY, now, "internal.", PageRequest.of(0, batchSize));

    if (batch.isEmpty()) return;

    for (OutboxEvent e : batch) {
      try {
        Object payload = deserialize(e.getEventType(), e.getPayloadJson());

        if (e.getMessageKey() == null || e.getMessageKey().isBlank()) {
          kafkaTemplate.send(e.getTopic(), payload).get(10, TimeUnit.SECONDS);
        } else {
          kafkaTemplate.send(e.getTopic(), e.getMessageKey(), payload).get(10, TimeUnit.SECONDS);
        }

        e.markSent();
        log.info("Outbox SENT id={}, topic={}, type={}", e.getId(), e.getTopic(), e.getEventType());

      } catch (Exception ex) {
        String err = safeError(ex);

        if (e.getAttempts() + 1 >= maxAttempts) {
          e.markDead(err);
          log.error(
              "Outbox DEAD id={}, topic={}, type={}, error={}",
              e.getId(),
              e.getTopic(),
              e.getEventType(),
              err);
        } else {
          Instant next = Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS);
          e.markRetry(err, next);
          log.warn(
              "Outbox RETRY id={}, attempt={}, nextAt={}, error={}",
              e.getId(),
              e.getAttempts(),
              next,
              err);
        }
      }
    }
    // Transactional: entity changes flush/commit ile DB’ye yazar.
  }

  private Object deserialize(String className, String json) throws Exception {
    Class<?> clazz = Class.forName(className);
    return objectMapper.readValue(json, clazz);
  }

  private String safeError(Exception ex) {
    String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
    return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
  }
}
