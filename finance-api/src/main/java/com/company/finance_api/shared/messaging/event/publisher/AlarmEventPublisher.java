package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.AlarmTriggeredEvent;

/** AlarmEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
public interface AlarmEventPublisher {

  /** {@link AlarmTriggeredEvent} olayını downstream consumer'lara publish eder. */
  void publish(AlarmTriggeredEvent event);
}
