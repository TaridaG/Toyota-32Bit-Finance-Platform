package com.company.notification.security.application;

import com.company.notification.alarm.infrastructure.email.AlarmEmailContent;
import com.company.notification.security.infrastructure.email.LoginSecurityEmailComposer;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
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
class SendLoginSecurityEmailUseCaseTest {

    @Mock
    private EmailSender emailSender;
    @Mock
    private LoginSecurityEmailComposer loginSecurityEmailComposer;

    @InjectMocks
    private SendLoginSecurityEmailUseCase useCase;

    @Test
    void handle_skips_when_no_recipient() {
        LoginSecurityAlertMessage event = new LoginSecurityAlertMessage();
        event.setUserId(UUID.randomUUID());
        event.setUserEmail(null);
        event.setRecipientEmail("");

        useCase.handle(event);

        verifyNoInteractions(loginSecurityEmailComposer, emailSender);
    }

    @Test
    void handle_prefers_recipient_email_over_user_email() {
        LoginSecurityAlertMessage event = new LoginSecurityAlertMessage();
        event.setRecipientEmail("  admin@example.com ");
        event.setUserEmail("user@example.com");

        AlarmEmailContent content = new AlarmEmailContent("Security", "<p>x</p>", "plain");
        when(loginSecurityEmailComposer.build(event)).thenReturn(content);

        useCase.handle(event);

        verify(emailSender).sendBrandedEmail(eq("admin@example.com"), eq("Security"), eq("<p>x</p>"), eq("plain"));
    }

    @Test
    void handle_falls_back_to_user_email() {
        LoginSecurityAlertMessage event = new LoginSecurityAlertMessage();
        event.setUserEmail(" user@example.com ");

        AlarmEmailContent content = new AlarmEmailContent("Security", "<p>x</p>", "plain");
        when(loginSecurityEmailComposer.build(event)).thenReturn(content);

        useCase.handle(event);

        verify(emailSender).sendBrandedEmail(eq("user@example.com"), any(), any(), any());
    }

    @Test
    void handle_propagates_composer_failure() {
        LoginSecurityAlertMessage event = new LoginSecurityAlertMessage();
        event.setUserEmail("user@example.com");
        when(loginSecurityEmailComposer.build(event)).thenThrow(new IllegalStateException("template error"));

        assertThrows(IllegalStateException.class, () -> useCase.handle(event));
        verify(emailSender, never()).sendBrandedEmail(any(), any(), any(), any());
    }
}
