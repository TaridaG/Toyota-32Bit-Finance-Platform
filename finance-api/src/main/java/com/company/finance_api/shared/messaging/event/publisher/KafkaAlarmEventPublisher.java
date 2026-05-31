package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.AlarmTriggeredEvent;
import com.company.finance_api.outbox.application.OutboxService;
import com.company.finance_api.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Alarm tetikleme event'lerini Kafka topic'ine publish eder. */
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaAlarmEventPublisher implements AlarmEventPublisher {

  private final OutboxService outboxService;

  /** {@link AlarmTriggeredEvent} kaydını outbox üzerinden Kafka topic'ine publish eder. */
  @Override
  public void publish(AlarmTriggeredEvent event) {
    // key olarak userId veya instrumentSymbol kullanılabilir; partitioning için mantıklı
    String key = event.getUserId() == null ? null : event.getUserId().toString();
    outboxService.enqueue(KafkaTopics.ALARM_TRIGGERED, key, event);
  }
}
