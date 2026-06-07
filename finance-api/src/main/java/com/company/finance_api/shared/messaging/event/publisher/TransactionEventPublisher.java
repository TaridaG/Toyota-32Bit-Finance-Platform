package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.TransactionExecutedEvent;

/** TransactionEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
public interface TransactionEventPublisher {
  /** {@link TransactionExecutedEvent} olayını downstream consumer'lara publish eder. */
  void publish(TransactionExecutedEvent event);
}
