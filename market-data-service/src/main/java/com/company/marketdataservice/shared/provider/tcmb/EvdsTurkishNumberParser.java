package com.company.marketdataservice.shared.provider.tcmb;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;


/**
 * TCMB EVDS yanıtlarındaki Türkçe sayı formatını parse eder.
 */
public final class EvdsTurkishNumberParser {

    private EvdsTurkishNumberParser() {
    }

    /**
     * Ham yanıtı parse eder.
         * @param raw girdi parametresi
         * @return işlem sonucu
         */
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
