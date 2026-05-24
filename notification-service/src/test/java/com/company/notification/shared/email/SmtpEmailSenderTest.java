package com.company.notification.shared.email;

import com.company.notification.bootstrap.config.NotificationMailProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;

    private SmtpEmailSender smtpEmailSender;

    @BeforeEach
    void setUp() {
        NotificationMailProperties props = new NotificationMailProperties();
        props.setFrom("noreply@example.com");
        smtpEmailSender = new SmtpEmailSender(mailSender, props);
    }

    @Test
    void sendEmail_sets_recipient_subject_and_from() {
        smtpEmailSender.sendEmail("user@example.com", "Hello", "Body text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertEquals("user@example.com", msg.getTo()[0]);
        assertEquals("Hello", msg.getSubject());
        assertEquals("Body text", msg.getText());
        assertEquals("noreply@example.com", msg.getFrom());
    }

    @Test
    void sendEmail_wraps_messaging_errors() {
        doThrow(new IllegalStateException("mail down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(IllegalStateException.class, () ->
                smtpEmailSender.sendEmail("user@example.com", "Subject", "Body"));
    }

    @Test
    void sendBrandedEmail_delegates_to_mime_sender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        smtpEmailSender.sendBrandedEmail("user@example.com", "Subject", "<b>html</b>", "plain");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendBrandedEmail_wraps_messaging_errors() {
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("mail down"));

        assertThrows(IllegalStateException.class, () ->
                smtpEmailSender.sendBrandedEmail("user@example.com", "S", "<p></p>", "p"));
    }
}
