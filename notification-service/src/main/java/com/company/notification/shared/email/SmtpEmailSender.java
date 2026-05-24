package com.company.notification.shared.email;

import com.company.notification.bootstrap.config.NotificationMailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Spring JavaMailSender kullanan {@link EmailSender} implementasyonu.
 */
@Service
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private static final String LOGO_RESOURCE = "email/site-logo.png";
    private static final String LOGO_CONTENT_ID = "portalLogo";

    private final JavaMailSender mailSender;
    private final NotificationMailProperties mailProperties;

    @Override
    public void sendEmail(String to, String subject, String plainBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(plainBody);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send plain email", ex);
        }
    }

    @Override
    public void sendBrandedEmail(String to, String subject, String htmlBody, String plainBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (StringUtils.hasText(mailProperties.getFrom())) {
                helper.setFrom(mailProperties.getFrom().trim());
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainBody, htmlBody);
            ClassPathResource logo = new ClassPathResource(LOGO_RESOURCE);
            if (logo.exists()) {
                helper.addInline(LOGO_CONTENT_ID, logo, "image/png");
            }
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send branded email", ex);
        }
    }

    private void applyFrom(SimpleMailMessage message) {
        if (StringUtils.hasText(mailProperties.getFrom())) {
            message.setFrom(mailProperties.getFrom().trim());
        }
    }
}
