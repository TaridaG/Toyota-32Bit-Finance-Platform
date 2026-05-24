package com.company.notification.shared.email;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Kullanıcı locale string'lerini desteklenen e-posta dillerine normalize eder ({@code en}, {@code tr}, {@code de}).
 */
public final class AlarmMailLocale {

    private AlarmMailLocale() {
    }

    /**
     * Şablon seçimi için desteklenen dil kodunu döner.
     */
    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "en";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains("-")) {
            value = value.substring(0, value.indexOf('-'));
        }
        if (value.contains("_")) {
            value = value.substring(0, value.indexOf('_'));
        }
        return switch (value) {
            case "tr", "de" -> value;
            default -> "en";
        };
    }
}
