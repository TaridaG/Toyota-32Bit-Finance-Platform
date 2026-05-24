package com.company.notification.report.infrastructure.email;

import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
import org.springframework.stereotype.Service;

/**
 * Rapor export bildirimleri için plain-text e-posta metinleri.
 */
@Service
public class ReportEmailComposer {

    /** Başarıyla üretilen rapor için subject satırı. */
    public String buildCompletedSubject(ReportCompletedMessage event) {
        return "Your report is ready";
    }

    /** Rapor metadata'sı ve indirme ipucunu listeleyen gövde metni. */
    public String buildCompletedBody(ReportCompletedMessage event) {

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
                event.getFileName()
        );
    }

    /** Rapor üretimi başarısız olduğunda subject satırı. */
    public String buildFailedSubject(ReportFailedMessage event) {
        return "Report generation failed";
    }

    /** Hata nedenini açıklayan gövde metni. */
    public String buildFailedBody(ReportFailedMessage event) {

        return """
                Report generation failed.

                Report ID: %s
                Type: %s
                Reason: %s

                Please try again later.
                """.formatted(
                event.getReportId(),
                event.getReportType(),
                event.getReason()
        );
    }
}