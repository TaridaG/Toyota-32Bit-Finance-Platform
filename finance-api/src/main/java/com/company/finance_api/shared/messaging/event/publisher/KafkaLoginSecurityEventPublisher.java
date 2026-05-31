package com.company.finance_api.shared.messaging.event.publisher;

import com.company.finance_api.shared.messaging.event.LoginSecurityAlertEvent;
import com.company.finance_api.outbox.application.OutboxService;
import com.company.finance_api.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** KafkaLoginSecurityEventPublisher — domain event'leri Kafka veya log kanalına publish eder. */
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class KafkaLoginSecurityEventPublisher implements LoginSecurityEventPublisher {

  private final OutboxService outboxService;

  @Override
  public void publish(LoginSecurityAlertEvent event) {
    String key = event.getUserId() == null ? null : event.getUserId().toString();
    outboxService.enqueue(KafkaTopics.LOGIN_SECURITY_ALERT, key, event);
  }
}
