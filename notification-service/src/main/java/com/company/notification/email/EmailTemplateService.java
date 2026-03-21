package com.company.notification.email;

import com.company.notification.event.ReportCompletedEvent;
import com.company.notification.event.ReportFailedEvent;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildCompletedSubject(ReportCompletedEvent event) {
        return "Your report is ready";
    }

    public String buildCompletedBody(ReportCompletedEvent event) {

        return """
                Your report has been generated successfully.

                Report ID: %s
                Type: %s
                Format: %s
                File: %s

                You can download it from the platform.

                """.formatted(
                event.getReportId(),
                event.getReportType(),
                event.getExportFormat(),
                event.getFileName(),
                event.getUserId(),
                event.getUserEmail()
        );
    }

    public String buildFailedSubject(ReportFailedEvent event) {
        return "Report generation failed";
    }

    public String buildFailedBody(ReportFailedEvent event) {

        return """
                Report generation failed.

                Report ID: %s
                Type: %s
                Reason: %s

                Please try again later.
                """.formatted(
                event.getReportId(),
                event.getReportType(),
                event.getReason(),
                event.getUserId(),
                event.getUserEmail()
        );
    }
}