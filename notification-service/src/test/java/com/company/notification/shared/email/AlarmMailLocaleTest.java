package com.company.notification.shared.email;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlarmMailLocaleTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void normalize_blank_defaults_to_en(String raw) {
        assertEquals("en", AlarmMailLocale.normalize(raw));
    }

    @ParameterizedTest
    @CsvSource({
            "en, en",
            "EN, en",
            "tr, tr",
            "TR, tr",
            "de, de",
            "tr-TR, tr",
            "de_DE, de",
            "en-US, en",
            "fr, en",
            "ja, en"
    })
    void normalize_supported_and_fallback(String input, String expected) {
        assertEquals(expected, AlarmMailLocale.normalize(input));
    }
}
