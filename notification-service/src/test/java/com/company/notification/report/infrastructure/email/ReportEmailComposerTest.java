package com.company.notification.report.infrastructure.email;

import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportEmailComposerTest {

    private final ReportEmailComposer composer = new ReportEmailComposer();

    @Test
    void buildCompleted_includes_report_metadata() {
        ReportCompletedMessage event = new ReportCompletedMessage();
        UUID reportId = UUID.randomUUID();
        event.setReportId(reportId);
        event.setReportType("PORTFOLIO");
        event.setExportFormat("PDF");
        event.setFileName("my-report.pdf");

        assertEquals("Your report is ready", composer.buildCompletedSubject(event));

        String body = composer.buildCompletedBody(event);
        assertTrue(body.contains(reportId.toString()));
        assertTrue(body.contains("PORTFOLIO"));
        assertTrue(body.contains("PDF"));
        assertTrue(body.contains("my-report.pdf"));
    }

    @Test
    void buildFailed_includes_reason() {
        ReportFailedMessage event = new ReportFailedMessage();
        event.setReportId(UUID.randomUUID());
        event.setReportType("TAX");
        event.setReason("export engine unavailable");

        assertEquals("Report generation failed", composer.buildFailedSubject(event));

        String body = composer.buildFailedBody(event);
        assertTrue(body.contains("TAX"));
        assertTrue(body.contains("export engine unavailable"));
    }
}
