package com.company.finance_api.event.publisher;

import com.company.finance_api.event.TransactionExecutedEvent;
import com.company.finance_api.outbox.application.OutboxService;
import com.company.finance_api.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** KafkaTransactionEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

  private final OutboxService outboxService;

  @Override
  public void publish(TransactionExecutedEvent event) {
    String key = event.getUserId() == null ? null : event.getUserId().toString();
    outboxService.enqueue(KafkaTopics.TRANSACTION_EXECUTED, key, event);
  }
}
