package com.company.finance_api.event.publisher;

import com.company.finance_api.event.LoginSecurityAlertEvent;

/** LoginSecurityEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
public interface LoginSecurityEventPublisher {

  /** publish sözleşmesi. */
  void publish(LoginSecurityAlertEvent event);
}
