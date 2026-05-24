package com.company.notification.alarm.application;

import com.company.notification.alarm.infrastructure.email.AlarmEmailComposer;
import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import com.company.notification.shared.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fiyat alarmı tetiklendiğinde markalı e-posta gönderir.
 * Event'te kullanıcı e-postası yoksa teslimatı atlar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendAlarmEmailUseCase {

    private final EmailSender emailSender;
    private final AlarmEmailComposer alarmEmailComposer;

    
    /** Event'ten alarm e-postasını oluşturur ve SMTP ile gönderir. */
    public void handle(AlarmTriggeredMessage event) {
        log.info(
                "ALARM_NOTIFICATION alarmId={}, userId={}, instrument={}, condition={}, price={}",
                event.getAlarmId(),
                event.getUserId(),
                event.getInstrumentSymbol(),
                event.getCondition(),
                event.getPrice()
        );

        if (!StringUtils.hasText(event.getUserEmail())) {
            log.warn("ALARM_EMAIL_SKIPPED alarmId={} reason=no_user_email", event.getAlarmId());
            return;
        }

        try {
            var content = alarmEmailComposer.build(event);
            emailSender.sendBrandedEmail(
                    event.getUserEmail().trim(),
                    content.subject(),
                    content.htmlBody(),
                    content.plainBody()
            );
        } catch (Exception ex) {
            log.error("ALARM_EMAIL_FAILED alarmId={} userId={}", event.getAlarmId(), event.getUserId(), ex);
            throw ex;
        }
    }
}
