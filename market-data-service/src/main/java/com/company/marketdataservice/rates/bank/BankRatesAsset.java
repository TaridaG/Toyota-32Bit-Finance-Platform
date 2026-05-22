package com.company.marketdataservice.rates.bank;

import java.util.Locale;

public enum BankRatesAsset {
    USD,
    EUR,
    GBP,
    GOLD;

    public static BankRatesAsset parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return USD;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "USD", "DOLAR", "DOLLAR" -> USD;
            case "EUR", "EURO" -> EUR;
            case "GBP", "STERLIN", "STERLING" -> GBP;
            case "GOLD", "ALTIN", "GRAM_ALTIN", "GRAM-ALTIN", "XAU" -> GOLD;
            default -> throw new IllegalArgumentException("Unsupported asset: " + raw);
        };
    }
}
