package com.company.finance_api.event.publisher;

import com.company.finance_api.event.AlarmTriggeredEvent;

/** AlarmEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
public interface AlarmEventPublisher {

  /** publish sözleşmesi. */
  void publish(AlarmTriggeredEvent event);
}
