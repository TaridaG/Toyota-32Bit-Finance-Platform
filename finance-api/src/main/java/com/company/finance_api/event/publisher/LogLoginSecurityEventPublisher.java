package com.company.finance_api.event.publisher;

import com.company.finance_api.event.LoginSecurityAlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** LogLoginSecurityEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
@Component
@Profile({"dev", "test"})
public class LogLoginSecurityEventPublisher implements LoginSecurityEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LogLoginSecurityEventPublisher.class);

  @Override
  public void publish(LoginSecurityAlertEvent event) {
    log.info(
        "LOGIN_SECURITY_EVENT userId={}, type={}, email={}, ip={}, at={}",
        event.getUserId(),
        event.getAlertType(),
        event.getUserEmail(),
        event.getClientIp(),
        event.getOccurredAt());
  }
}
