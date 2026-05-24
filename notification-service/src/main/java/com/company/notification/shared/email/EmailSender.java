package com.company.notification.shared.email;

/**
 * Giden e-posta port'u (plain veya inline branding'li HTML multipart).
 */
public interface EmailSender {

    /** Basit plain-text mesaj gönderir. */
    void sendEmail(String to, String subject, String plainBody);

    /** Plain-text alternatifli ve isteğe bağlı inline logo'lu multipart HTML gönderir. */
    void sendBrandedEmail(String to, String subject, String htmlBody, String plainBody);
}
