package com.company.notification.security.infrastructure.kafka;

import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.security.application.SendLoginSecurityEmailUseCase;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
import com.company.notification.shared.kafka.KafkaCorrelationSupport;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code login-security.alert} event'leri için Kafka inbound adapter.
 */
@Component
@RequiredArgsConstructor
public class LoginSecurityAlertEventConsumer {

    private final SendLoginSecurityEmailUseCase sendLoginSecurityEmailUseCase;

    /**
     * Kaydı {@link SendLoginSecurityEmailUseCase}'e iletir.
     */
    @KafkaListener(
            topics = KafkaTopicNames.LOGIN_SECURITY_ALERT,
            groupId = "notification-service",
            containerFactory = "loginSecurityKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, LoginSecurityAlertMessage> record) {
        KafkaCorrelationSupport.runWithCorrelation(record, () ->
                sendLoginSecurityEmailUseCase.handle(record.value()));
    }
}
