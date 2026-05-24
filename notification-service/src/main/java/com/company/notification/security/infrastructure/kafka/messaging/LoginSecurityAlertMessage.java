package com.company.notification.security.infrastructure.kafka.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Login ve hesap güvenliği bildirimleri için Kafka payload.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginSecurityAlertMessage {

    private UUID userId;
    private String userEmail;
    private String recipientEmail;
    private String username;
    private String preferredLocale;
    private String alertType;
    private Instant occurredAt;
    private String clientIp;
    private String userAgent;
    private String previousEmail;
    private String newEmail;
    private String previousUsername;
    private String newUsername;
    private String adminMessageBody;
}
