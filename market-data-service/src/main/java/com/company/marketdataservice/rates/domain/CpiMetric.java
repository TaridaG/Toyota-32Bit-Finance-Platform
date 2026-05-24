package com.company.marketdataservice.rates.domain;
import org.springframework.util.StringUtils;

/**
 * `makro oran` domain katmanı tipi veya port arayüzü.
 */
public enum CpiMetric {
    INDEX,
    MONTHLY_PCT,
    YEARLY_PCT;

    /**
     * Ham yanıtı parse eder.
         * @param raw girdi parametresi
         * @return işlem sonucu
         */
    public static CpiMetric parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return YEARLY_PCT;
        }
        return switch (raw.trim().toUpperCase()) {
            case "INDEX", "ENDEKS" -> INDEX;
            case "MONTHLY_PCT", "MONTHLY", "AYLIK" -> MONTHLY_PCT;
            case "YEARLY_PCT", "YEARLY", "YILLIK" -> YEARLY_PCT;
            default -> throw new IllegalArgumentException("Unsupported CPI metric: " + raw);
        };
    }
}
