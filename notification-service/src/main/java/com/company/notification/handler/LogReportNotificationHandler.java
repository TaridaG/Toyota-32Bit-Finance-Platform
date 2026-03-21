package com.company.notification.handler;

import com.company.notification.email.EmailService;
import com.company.notification.email.EmailTemplateService;
import com.company.notification.event.ReportCompletedEvent;
import com.company.notification.event.ReportFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogReportNotificationHandler implements ReportNotificationHandler {
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Override
    public void handleCompleted(ReportCompletedEvent event) {

        log.info(
                "REPORT_COMPLETED reportId={} type={} format={}",
                event.getReportId(),
                event.getReportType(),
                event.getExportFormat(),
                event.getUserId(),
                event.getUserEmail()
        );

        String subject =
                emailTemplateService.buildCompletedSubject(event);

        String body =
                emailTemplateService.buildCompletedBody(event);

        emailService.sendEmail(
                event.getUserEmail(),
                subject,
                body
        );
    }

    @Override
    public void handleFailed(ReportFailedEvent event) {

        log.warn(
                "REPORT_FAILED reportId={} reason={}",
                event.getReportId(),
                event.getReason(),
                event.getUserId(),
                event.getUserEmail()
        );

        String subject =
                emailTemplateService.buildFailedSubject(event);

        String body =
                emailTemplateService.buildFailedBody(event);

        emailService.sendEmail(
                event.getUserEmail(),
                subject,
                body
        );
    }
}