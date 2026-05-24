package com.company.notification.security.infrastructure.kafka;

import com.company.notification.security.application.SendLoginSecurityEmailUseCase;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginSecurityAlertEventConsumerTest {

    @Mock
    private SendLoginSecurityEmailUseCase sendLoginSecurityEmailUseCase;

    @InjectMocks
    private LoginSecurityAlertEventConsumer consumer;

    @Test
    void consume_delegates_to_use_case() {
        LoginSecurityAlertMessage message = new LoginSecurityAlertMessage();
        message.setAlertType("LOGIN_FAILED");
        message.setUserEmail("user@example.com");
        ConsumerRecord<String, LoginSecurityAlertMessage> record =
                new ConsumerRecord<>("login-security.alert", 0, 0L, "key", message);

        consumer.consume(record);

        verify(sendLoginSecurityEmailUseCase).handle(message);
    }
}
