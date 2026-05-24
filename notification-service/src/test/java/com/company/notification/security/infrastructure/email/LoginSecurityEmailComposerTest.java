package com.company.notification.security.infrastructure.email;

import com.company.notification.alarm.infrastructure.email.AlarmEmailContent;
import com.company.notification.bootstrap.config.NotificationMailProperties;
import com.company.notification.security.infrastructure.kafka.messaging.LoginSecurityAlertMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoginSecurityEmailComposerTest {

    private LoginSecurityEmailComposer composer;

    @BeforeEach
    void setUp() {
        NotificationMailProperties props = new NotificationMailProperties();
        props.setPortalPublicUrl("https://portal.example.com");
        composer = new LoginSecurityEmailComposer(props);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "LOGIN_FAILED",
            "PASSWORD_CHANGED",
            "USERNAME_CHANGED",
            "EMAIL_CHANGED_OLD_ACCOUNT",
            "EMAIL_CHANGED_NEW_ACCOUNT",
            "ACCOUNT_FROZEN",
            "ACCOUNT_UNFROZEN",
            "ADMIN_MESSAGE",
            "ACCOUNT_DELETED_BY_ADMIN",
            "REGISTRATION_EMAIL_UNBLOCKED"
    })
    void build_produces_content_for_known_alert_types(String alertType) {
        LoginSecurityAlertMessage event = baseEvent();
        event.setAlertType(alertType);
        event.setAdminMessageBody("Admin notice");
        event.setPreviousUsername("oldUser");
        event.setNewUsername("newUser");
        event.setPreviousEmail("old@example.com");
        event.setNewEmail("new@example.com");

        AlarmEmailContent content = composer.build(event);

        assertNotNull(content);
        assertFalse(content.subject().isBlank());
        assertFalse(content.htmlBody().isBlank());
        assertFalse(content.plainBody().isBlank());
        assertTrue(content.htmlBody().contains("<!DOCTYPE html>"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOGIN_FAILED", "PASSWORD_CHANGED"})
    void build_turkish_locale(String alertType) {
        LoginSecurityAlertMessage event = baseEvent();
        event.setAlertType(alertType);
        event.setPreferredLocale("tr");

        AlarmEmailContent content = composer.build(event);

        assertFalse(content.subject().isBlank());
        assertTrue(content.htmlBody().contains("lang=\"tr\""));
    }

    private static LoginSecurityAlertMessage baseEvent() {
        LoginSecurityAlertMessage event = new LoginSecurityAlertMessage();
        event.setUserId(UUID.randomUUID());
        event.setUserEmail("user@example.com");
        event.setUsername("trader1");
        event.setOccurredAt(Instant.parse("2024-06-01T12:00:00Z"));
        event.setClientIp("203.0.113.10");
        event.setUserAgent("Mozilla/5.0");
        return event;
    }
}
