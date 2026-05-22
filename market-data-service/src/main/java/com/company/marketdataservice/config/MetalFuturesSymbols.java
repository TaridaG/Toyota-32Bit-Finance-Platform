package com.company.marketdataservice.config;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Yahoo international metal futures ({@code GC=F}, …) and TRY spot pairs for spread. */
public final class MetalFuturesSymbols {

    private MetalFuturesSymbols() {}

    public static final Set<String> FUTURES = Set.of("GC=F", "SI=F", "HG=F", "PA=F", "PL=F");

    public static final Map<String, String> FUTURES_TO_SPOT_TRY = Map.of(
            "GC=F", "XAUTRY",
            "SI=F", "XAGTRY",
            "PL=F", "XPTTRY",
            "PA=F", "XPDTRY",
            "HG=F", "XCUTRY");

    public static boolean isFutures(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        return FUTURES.contains(symbol.trim().toUpperCase(Locale.ROOT));
    }

    public static String linkedSpotTry(String futuresSymbol) {
        if (futuresSymbol == null) {
            return null;
        }
        return FUTURES_TO_SPOT_TRY.get(futuresSymbol.trim().toUpperCase(Locale.ROOT));
    }
}
