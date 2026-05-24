package com.company.notification.alarm.infrastructure.email;

import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import com.company.notification.bootstrap.config.NotificationMailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AlarmEmailComposerTest {

    private AlarmEmailComposer composer;

    @BeforeEach
    void setUp() {
        NotificationMailProperties props = new NotificationMailProperties();
        props.setPortalPublicUrl("https://portal.example.com");
        composer = new AlarmEmailComposer(props);
    }

    @Test
    void build_includes_symbol_in_subject_and_body() {
        AlarmTriggeredMessage event = baseEvent();
        event.setInstrumentSymbol("AAPL");
        event.setPreferredLocale("en");
        event.setCondition("GREATER_THAN");

        AlarmEmailContent content = composer.build(event);

        assertTrue(content.subject().contains("AAPL"));
        assertTrue(content.htmlBody().contains("AAPL"));
        assertTrue(content.plainBody().contains("AAPL"));
        assertTrue(content.htmlBody().contains("Price above target"));
    }

    @Test
    void build_turkish_locale_uses_turkish_copy() {
        AlarmTriggeredMessage event = baseEvent();
        event.setInstrumentSymbol("THYAO");
        event.setPreferredLocale("tr-TR");
        event.setCondition("PERCENT_CHANGE_DOWN");

        AlarmEmailContent content = composer.build(event);

        assertTrue(content.subject().contains("THYAO"));
        assertTrue(content.plainBody().contains("Yüzde düşüş") || content.htmlBody().contains("Yüzde"));
    }

    @ParameterizedTest
    @CsvSource({
            "GREATER_THAN, en, Price above target",
            "LESS_THAN, tr, Fiyat hedefin altında",
            "EQUAL, de, Kurs gleich Schwelle"
    })
    void conditionLabel_localized(String condition, String lang, String expectedFragment) {
        String label = AlarmEmailComposer.conditionLabel(lang, condition);
        assertEquals(expectedFragment, label);
    }

    private static AlarmTriggeredMessage baseEvent() {
        AlarmTriggeredMessage event = new AlarmTriggeredMessage();
        event.setThreshold(new BigDecimal("100.5"));
        event.setPrice(new BigDecimal("105"));
        event.setTriggeredAt(Instant.parse("2024-06-01T10:00:00Z"));
        return event;
    }
}
