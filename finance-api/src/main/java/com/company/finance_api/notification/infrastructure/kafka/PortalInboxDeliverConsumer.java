package com.company.finance_api.notification.infrastructure.kafka;

import com.company.finance_api.notification.application.DeliverPortalInboxFromKafkaUseCase;
import com.company.finance_api.notification.infrastructure.kafka.messaging.PortalInboxDeliverMessage;
import com.company.finance_api.shared.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** notification.portal.inbox topic'inden gelen mesajları consume eder. */
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class PortalInboxDeliverConsumer {

  private final DeliverPortalInboxFromKafkaUseCase deliverPortalInboxFromKafkaUseCase;

  @KafkaListener(
      topics = KafkaTopics.NOTIFICATION_PORTAL_INBOX,
      groupId = "finance-api-portal-inbox",
      containerFactory = "portalInboxKafkaListenerContainerFactory")
  public void consume(ConsumerRecord<String, PortalInboxDeliverMessage> record) {
    deliverPortalInboxFromKafkaUseCase.handle(record.value());
  }
}
