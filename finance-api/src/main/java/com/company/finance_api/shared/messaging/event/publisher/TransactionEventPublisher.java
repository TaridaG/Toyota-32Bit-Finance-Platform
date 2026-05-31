package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.TransactionExecutedEvent;

/** TransactionEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
public interface TransactionEventPublisher {
  /** publish sözleşmesi. */
  void publish(TransactionExecutedEvent event);
}
