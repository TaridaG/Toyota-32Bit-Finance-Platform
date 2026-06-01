package com.company.notification.insight.infrastructure.kafka;

import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.insight.infrastructure.kafka.messaging.PortalInboxDeliverMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Portal bildirim kutusu kayıtları için Kafka outbound adapter.
 */
@Component
@RequiredArgsConstructor
public class PortalInboxKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PortalInboxKafkaPublisher.class);
    private static final String NOTIFICATION_TYPE = "WATCHLIST_DIGEST";

    private final KafkaTemplate<String, Object> dlqKafkaTemplate;

    public void publish(PortalInboxDeliverMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        message.setNotificationType(NOTIFICATION_TYPE);
        String key = message.getUserId().toString();
        dlqKafkaTemplate.send(KafkaTopicNames.NOTIFICATION_PORTAL_INBOX, key, message);
        log.info(
                "PORTAL_INBOX_PUBLISHED userId={} eventId={} symbol={}",
                message.getUserId(),
                message.getEventId(),
                message.getPrimarySymbol());
    }
}
