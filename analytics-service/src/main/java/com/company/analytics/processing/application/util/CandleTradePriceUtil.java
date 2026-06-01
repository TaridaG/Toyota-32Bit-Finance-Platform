package com.company.analytics.processing.application.util;

import java.math.BigDecimal;

/** OHLC aggregation için geçerli trade fiyatı kontrolleri. */
public final class CandleTradePriceUtil {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private CandleTradePriceUtil() {
    }

    public static boolean isValidTradePrice(BigDecimal price) {
        return price != null && price.compareTo(ZERO) > 0;
    }
}
