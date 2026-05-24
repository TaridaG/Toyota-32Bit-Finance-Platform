package com.company.notification.report.application;

import com.company.notification.report.infrastructure.email.ReportEmailComposer;
import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
import com.company.notification.shared.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tamamlanan veya başarısız rapor export'ları için plain-text e-posta gönderir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendReportEmailUseCase implements ReportNotificationUseCase {

    private final EmailSender emailSender;
    private final ReportEmailComposer reportEmailComposer;

    /** {@inheritDoc} */
    @Override
    public void handleCompleted(ReportCompletedMessage event) {
        log.info(
                "REPORT_COMPLETED reportId={} type={} format={} userId={} email={}",
                event.getReportId(),
                event.getReportType(),
                event.getExportFormat(),
                event.getUserId(),
                event.getUserEmail()
        );

        String to = resolveRecipientEmail(event.getUserEmail());
        if (!StringUtils.hasText(to)) {
            log.warn("REPORT_EMAIL_SKIPPED reportId={} reason=no_user_email", event.getReportId());
            return;
        }

        try {
            emailSender.sendEmail(
                    to,
                    reportEmailComposer.buildCompletedSubject(event),
                    reportEmailComposer.buildCompletedBody(event)
            );
        } catch (Exception ex) {
            log.error("REPORT_EMAIL_FAILED reportId={} userId={}", event.getReportId(), event.getUserId(), ex);
            throw ex;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleFailed(ReportFailedMessage event) {
        log.warn(
                "REPORT_FAILED reportId={} reason={} userId={} email={}",
                event.getReportId(),
                event.getReason(),
                event.getUserId(),
                event.getUserEmail()
        );

        String to = resolveRecipientEmail(event.getUserEmail());
        if (!StringUtils.hasText(to)) {
            log.warn("REPORT_EMAIL_SKIPPED reportId={} reason=no_user_email", event.getReportId());
            return;
        }

        try {
            emailSender.sendEmail(
                    to,
                    reportEmailComposer.buildFailedSubject(event),
                    reportEmailComposer.buildFailedBody(event)
            );
        } catch (Exception ex) {
            log.error("REPORT_EMAIL_FAILED reportId={} userId={}", event.getReportId(), event.getUserId(), ex);
            throw ex;
        }
    }

    private String resolveRecipientEmail(String userEmail) {
        return StringUtils.hasText(userEmail) ? userEmail.trim() : null;
    }
}
