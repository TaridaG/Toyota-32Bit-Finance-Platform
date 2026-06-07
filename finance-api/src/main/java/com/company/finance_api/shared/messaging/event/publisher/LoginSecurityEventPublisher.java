package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.LoginSecurityAlertEvent;

/** LoginSecurityEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
public interface LoginSecurityEventPublisher {

  /** {@link LoginSecurityAlertEvent} olayını downstream consumer'lara publish eder. */
  void publish(LoginSecurityAlertEvent event);
}
