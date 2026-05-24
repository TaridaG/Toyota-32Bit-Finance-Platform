package com.company.notification.alarm.infrastructure.email;

/**
 * Teslimata hazır render edilmiş alarm e-postası (subject, HTML ve plain-text gövde).
 */
public record AlarmEmailContent(
        String subject,
        String htmlBody,
        String plainBody
) {
}
