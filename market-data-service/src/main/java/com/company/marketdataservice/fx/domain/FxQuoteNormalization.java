package com.company.marketdataservice.fx.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Canonical FX quote scale for storage, history, and % change (mirrors finance-api
 * {@code CurrencyConversionServiceImpl#normalizeFxMidForTryHub}).
 *
 * <p>TCMB publishes {@code JPYTRY} as TRY per 100 JPY (~28); the platform hub uses TRY per 1 JPY (~0.28).
 */
public final class FxQuoteNormalization {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal JPYUSD_RESCALE_LOW = new BigDecimal("0.03");

    private FxQuoteNormalization() {}

    public static String canonicalSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "";
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    public static BigDecimal normalizePrice(String symbol, BigDecimal raw) {
        if (raw == null || raw.compareTo(BigDecimal.ZERO) <= 0) {
            return raw;
        }
        String canonical = canonicalSymbol(symbol);
        if ("JPYTRY".equals(canonical)) {
            if (raw.compareTo(ONE) > 0) {
                return raw.divide(HUNDRED, 12, RoundingMode.HALF_UP);
            }
            return raw;
        }
        if ("JPYUSD".equals(canonical)) {
            BigDecimal r = raw;
            if (r.compareTo(TEN) > 0) {
                r = ONE.divide(r, 12, RoundingMode.HALF_UP);
            }
            if (r.compareTo(JPYUSD_RESCALE_LOW) > 0 && r.compareTo(ONE) < 0) {
                r = r.divide(HUNDRED, 12, RoundingMode.HALF_UP);
            }
            return r;
        }
        return raw;
    }
}
