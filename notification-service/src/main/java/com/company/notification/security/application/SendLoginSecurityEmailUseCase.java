package com.company.notification.security.application;

import com.company.notification.shared.email.EmailSender;
import com.company.notification.security.infrastructure.email.LoginSecurityEmailComposer;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Markalı güvenlik e-postalarını teslim eder (login alert, hesap değişiklikleri, admin bildirimleri).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendLoginSecurityEmailUseCase {

    private final EmailSender emailSender;
    private final LoginSecurityEmailComposer loginSecurityEmailComposer;

    /**
     * Alıcıyı belirler, şablonu render eder ve e-postayı gönderir.
     */
    public void handle(LoginSecurityAlertMessage event) {
        log.info(
                "LOGIN_SECURITY_NOTIFICATION userId={}, type={}, ip={}",
                event.getUserId(),
                event.getAlertType(),
                event.getClientIp()
        );

        String to = StringUtils.hasText(event.getRecipientEmail())
                ? event.getRecipientEmail().trim()
                : event.getUserEmail() != null ? event.getUserEmail().trim() : null;
        if (!StringUtils.hasText(to)) {
            log.warn("LOGIN_SECURITY_EMAIL_SKIPPED userId={} reason=no_recipient", event.getUserId());
            return;
        }

        try {
            var content = loginSecurityEmailComposer.build(event);
            emailSender.sendBrandedEmail(
                    to,
                    content.subject(),
                    content.htmlBody(),
                    content.plainBody()
            );
        } catch (Exception ex) {
            log.error("LOGIN_SECURITY_EMAIL_FAILED userId={} type={}", event.getUserId(), event.getAlertType(), ex);
            throw ex;
        }
    }
}
