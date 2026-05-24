package com.company.marketdataservice.rates.infrastructure.provider.bank;
import java.util.Locale;

/**
 * `makro oran` infrastructure katmanı adaptörü.
 */
public enum BankRatesAsset {
    USD,
    EUR,
    GBP,
    GOLD;

    /**
     * Ham yanıtı parse eder.
         * @param raw girdi parametresi
         * @return işlem sonucu
         */
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
