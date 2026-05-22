package com.company.finance_api.registration;

import org.springframework.util.StringUtils;

import java.util.Locale;

public final class VerificationMailLocale {

    private VerificationMailLocale() {
    }

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
