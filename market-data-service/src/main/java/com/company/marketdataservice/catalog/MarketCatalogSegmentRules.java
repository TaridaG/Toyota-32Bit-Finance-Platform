package com.company.marketdataservice.catalog;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Mirrors {@code frontend-web/src/features/markets/api/marketService.ts} classification so
 * segment pulse buckets stay consistent with the Markets table filters.
 */
public final class MarketCatalogSegmentRules {

    private static final Set<String> FUND_SYMBOLS = Set.of("VOO", "VTI", "QQQ", "IVV", "SPY");
    private static final Set<String> SPOT_METAL_SYMBOLS =
            Set.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");
    private static final Set<String> METAL_FUTURES_SYMBOLS = Set.of("GC=F", "SI=F", "HG=F", "PA=F", "PL=F");
    private static final Set<String> METAL_SYMBOLS;

    static {
        var union = new HashSet<>(SPOT_METAL_SYMBOLS);
        union.addAll(METAL_FUTURES_SYMBOLS);
        METAL_SYMBOLS = Collections.unmodifiableSet(union);
    }

    private MarketCatalogSegmentRules() {}

    /**
     * Coarse wire category (CRYPTO, STOCK, FX, FUND, METAL, BOND) aligned with the SPA catalog merge.
     */
    public static String inferWireCategory(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "STOCK";
        }
        String s = symbol.trim().toUpperCase(Locale.ROOT);
        if (s.startsWith("TRBOND") || s.startsWith("TRGOVUSD")) {
            return "BOND";
        }
        if (s.startsWith("FUND_")) {
            return "FUND";
        }
        if (METAL_SYMBOLS.contains(s)) {
            return "METAL";
        }
        if (FUND_SYMBOLS.contains(s)) {
            return "FUND";
        }
        if (s.endsWith("USDT") || s.endsWith("USD")) {
            return "CRYPTO";
        }
        if (s.endsWith("TRY") || s.contains("/")) {
            return "FX";
        }
        return "STOCK";
    }

    /**
     * UI pulse segment id (matches {@code MarketCategory} minus {@code all}), or {@code null} if out of scope.
     */
    public static String pulseSegment(String symbol, String wireCategory, String source) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        String s = symbol.trim().toUpperCase(Locale.ROOT);
        String cat = wireCategory == null ? "" : wireCategory.trim().toUpperCase(Locale.ROOT);
        String src = source == null ? "" : source.trim().toUpperCase(Locale.ROOT);

        if ("CRYPTO".equals(cat)) {
            return "crypto";
        }
        if ("FUND".equals(cat)) {
            return "funds";
        }
        if ("BOND".equals(cat)) {
            return "bonds";
        }
        if ("METAL".equals(cat)) {
            if (SPOT_METAL_SYMBOLS.contains(s)) {
                return "metals";
            }
            if (METAL_FUTURES_SYMBOLS.contains(s)) {
                return "globalFutures";
            }
            return null;
        }
        if ("FX".equals(cat)) {
            if (SPOT_METAL_SYMBOLS.contains(s)) {
                return "metals";
            }
            if (METAL_FUTURES_SYMBOLS.contains(s)) {
                return "globalFutures";
            }
            return "forex";
        }
        if ("STOCK".equals(cat)) {
            if ("YAHOO".equals(src)) {
                return "bist";
            }
            if ("FINNHUB".equals(src)) {
                return "nasdaq";
            }
            return null;
        }
        return null;
    }

    public static boolean usesFxHistoryPath(String symbol, String wireCategory) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        String s = symbol.trim().toUpperCase(Locale.ROOT);
        String cat = wireCategory == null ? "" : wireCategory.trim().toUpperCase(Locale.ROOT);
        return "FX".equals(cat) || SPOT_METAL_SYMBOLS.contains(s);
    }
}
