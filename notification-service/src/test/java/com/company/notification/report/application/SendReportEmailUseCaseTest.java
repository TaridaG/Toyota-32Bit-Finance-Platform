package com.company.notification.report.application;

import com.company.notification.report.infrastructure.email.ReportEmailComposer;
import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
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
class SendReportEmailUseCaseTest {

    @Mock
    private EmailSender emailSender;
    @Mock
    private ReportEmailComposer reportEmailComposer;

    @InjectMocks
    private SendReportEmailUseCase useCase;

    @Test
    void handleCompleted_sends_email() {
        ReportCompletedMessage event = new ReportCompletedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail("user@example.com");
        event.setReportType("PORTFOLIO");
        event.setExportFormat("PDF");
        event.setFileName("report.pdf");

        when(reportEmailComposer.buildCompletedSubject(event)).thenReturn("Ready");
        when(reportEmailComposer.buildCompletedBody(event)).thenReturn("Body");

        useCase.handleCompleted(event);

        verify(emailSender).sendEmail("user@example.com", "Ready", "Body");
    }

    @Test
    void handleCompleted_trims_user_email() {
        ReportCompletedMessage event = new ReportCompletedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail("  user@example.com  ");

        when(reportEmailComposer.buildCompletedSubject(event)).thenReturn("Ready");
        when(reportEmailComposer.buildCompletedBody(event)).thenReturn("Body");

        useCase.handleCompleted(event);

        verify(emailSender).sendEmail(eq("user@example.com"), eq("Ready"), eq("Body"));
    }

    @Test
    void handleCompleted_skips_when_user_email_missing() {
        ReportCompletedMessage event = new ReportCompletedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail("  ");

        useCase.handleCompleted(event);

        verifyNoInteractions(reportEmailComposer, emailSender);
    }

    @Test
    void handleCompleted_propagates_email_failure() {
        ReportCompletedMessage event = new ReportCompletedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail("user@example.com");

        when(reportEmailComposer.buildCompletedSubject(event)).thenReturn("Ready");
        when(reportEmailComposer.buildCompletedBody(event)).thenReturn("Body");
        doThrow(new IllegalStateException("mail down")).when(emailSender).sendEmail(any(), any(), any());

        assertThrows(IllegalStateException.class, () -> useCase.handleCompleted(event));
    }

    @Test
    void handleFailed_sends_email() {
        ReportFailedMessage event = new ReportFailedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail("user@example.com");
        event.setReportType("PORTFOLIO");
        event.setReason("timeout");

        when(reportEmailComposer.buildFailedSubject(event)).thenReturn("Failed");
        when(reportEmailComposer.buildFailedBody(event)).thenReturn("Error body");

        useCase.handleFailed(event);

        verify(emailSender).sendEmail("user@example.com", "Failed", "Error body");
    }

    @Test
    void handleFailed_skips_when_user_email_missing() {
        ReportFailedMessage event = new ReportFailedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail(null);

        useCase.handleFailed(event);

        verifyNoInteractions(reportEmailComposer, emailSender);
    }

    @Test
    void handleFailed_propagates_email_failure() {
        ReportFailedMessage event = new ReportFailedMessage();
        event.setReportId(UUID.randomUUID());
        event.setUserEmail("user@example.com");

        when(reportEmailComposer.buildFailedSubject(event)).thenReturn("Failed");
        when(reportEmailComposer.buildFailedBody(event)).thenReturn("Error body");
        doThrow(new IllegalStateException("mail down")).when(emailSender).sendEmail(any(), any(), any());

        assertThrows(IllegalStateException.class, () -> useCase.handleFailed(event));
    }
}
