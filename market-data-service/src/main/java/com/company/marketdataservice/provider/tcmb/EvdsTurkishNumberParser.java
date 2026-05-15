package com.company.marketdataservice.provider.tcmb;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * EVDS often returns Turkish-formatted decimals: {@code 37,50} or {@code 3.683,83} (dot = thousands, comma = fraction).
 */
public final class EvdsTurkishNumberParser {

    private EvdsTurkishNumberParser() {
    }

    public static BigDecimal parsePercentLike(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String t = raw.trim();
        if (t.contains(",")) {
            int ci = t.lastIndexOf(',');
            String before = t.substring(0, ci).replace(".", "");
            String after = t.substring(ci + 1).replace(".", "");
            t = before + "." + after;
        } else {
            t = t.replace(',', '.');
        }
        try {
            return new BigDecimal(t);
        } catch (Exception ex) {
            return null;
        }
    }
}
