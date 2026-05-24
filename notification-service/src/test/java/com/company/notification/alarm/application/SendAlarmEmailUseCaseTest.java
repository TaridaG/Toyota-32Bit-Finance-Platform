package com.company.notification.alarm.application;

import com.company.notification.alarm.infrastructure.email.AlarmEmailContent;
import com.company.notification.alarm.infrastructure.email.AlarmEmailComposer;
import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import com.company.notification.shared.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendAlarmEmailUseCaseTest {

    @Mock
    private EmailSender emailSender;
    @Mock
    private AlarmEmailComposer alarmEmailComposer;

    @InjectMocks
    private SendAlarmEmailUseCase useCase;

    @Test
    void handle_skips_when_user_email_missing() {
        AlarmTriggeredMessage event = new AlarmTriggeredMessage();
        event.setAlarmId(1L);
        event.setUserEmail("  ");

        useCase.handle(event);

        verifyNoInteractions(alarmEmailComposer, emailSender);
    }

    @Test
    void handle_sends_branded_email_when_email_present() {
        AlarmTriggeredMessage event = new AlarmTriggeredMessage();
        event.setAlarmId(2L);
        event.setUserId(UUID.randomUUID());
        event.setUserEmail("  user@example.com  ");

        AlarmEmailContent content = new AlarmEmailContent("Subject", "<p>html</p>", "plain");
        when(alarmEmailComposer.build(event)).thenReturn(content);

        useCase.handle(event);

        verify(alarmEmailComposer).build(event);
        verify(emailSender).sendBrandedEmail(
                eq("user@example.com"),
                eq("Subject"),
                eq("<p>html</p>"),
                eq("plain")
        );
    }

    @Test
    void handle_propagates_composer_failure() {
        AlarmTriggeredMessage event = new AlarmTriggeredMessage();
        event.setUserEmail("user@example.com");
        when(alarmEmailComposer.build(event)).thenThrow(new IllegalStateException("template error"));

        assertThrows(IllegalStateException.class, () -> useCase.handle(event));
        verify(emailSender, never()).sendBrandedEmail(any(), any(), any(), any());
    }
}
