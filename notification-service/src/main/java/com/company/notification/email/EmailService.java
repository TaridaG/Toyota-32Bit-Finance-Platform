package com.company.notification.email;

public interface EmailService {

    void sendEmail(
            String to,
            String subject,
            String body
    );
}