package com.company.finance_api.market;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Aligns finance-api market overview filters with {@code market-data-service}
 * {@link com.company.marketdataservice.catalog.MarketCatalogSegmentRules} and the SPA catalog.
 */
public final class MarketOverviewCategoryRules {

    private static final Set<String> FUND_SYMBOLS = Set.of("VOO", "VTI", "QQQ", "IVV", "SPY");
    private static final Set<String> SPOT_METAL_SYMBOLS =
            Set.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");
    private static final Set<String> METAL_FUTURES_SYMBOLS = Set.of("GC=F", "SI=F", "HG=F", "PA=F", "PL=F");

    public static final Map<String, String> METAL_FUTURES_TO_SPOT_TRY = Map.of(
            "GC=F", "XAUTRY",
            "SI=F", "XAGTRY",
            "PL=F", "XPTTRY",
            "PA=F", "XPDTRY",
            "HG=F", "XCUTRY");
    private static final Set<String> METAL_SYMBOLS;

    static {
        var union = new HashSet<>(SPOT_METAL_SYMBOLS);
        union.addAll(METAL_FUTURES_SYMBOLS);
        METAL_SYMBOLS = Collections.unmodifiableSet(union);
    }

    private MarketOverviewCategoryRules() {}

    /**
     * Quote currency for {@code X-Currency} conversion on overview rows (symbol-only).
     * Spot metals ({@code XAUTRY}, …) and {@code *TRY} FX crosses are quoted in TRY.
     */
    public static String listingCurrency(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return "USD";
        }
        String s = symbol.trim().toUpperCase(Locale.ROOT);
        if (s.startsWith("FUND_")) {
            return "TRY";
        }
        if (SPOT_METAL_SYMBOLS.contains(s)) {
            return "TRY";
        }
        if (s.length() == 6 && s.endsWith("TRY")) {
            return "TRY";
        }
        return "USD";
    }

    public static String inferWireCategory(String symbol) {
        if (!StringUtils.hasText(symbol)) {
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

    public static String pulseSegment(String symbol, String wireCategory, String source) {
        if (!StringUtils.hasText(symbol)) {
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

    public static boolean matchesUiCategory(String symbol, String source, String normalizedCategory) {
        if (!StringUtils.hasText(normalizedCategory) || "ALL".equalsIgnoreCase(normalizedCategory.trim())) {
            return true;
        }
        String mdsSegment = toMdsSegment(normalizedCategory);
        if (mdsSegment == null) {
            return true;
        }
        String wire = inferWireCategory(symbol);
        String pulse = pulseSegment(symbol, wire, source);
        return pulse != null && pulse.equalsIgnoreCase(mdsSegment);
    }

    public static String toMdsSegment(String normalizedCategory) {
        if (!StringUtils.hasText(normalizedCategory)) {
            return null;
        }
        String c = normalizedCategory.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(c)) {
            return null;
        }
        return switch (c) {
            case "CRYPTO" -> "crypto";
            case "BIST" -> "bist";
            case "NASDAQ" -> "nasdaq";
            case "FOREX", "FX" -> "forex";
            case "METALS" -> "metals";
            case "GLOBALFUTURES", "GLOBAL_FUTURES" -> "globalFutures";
            case "FUNDS", "FUND" -> "funds";
            case "BONDS", "BOND" -> "bonds";
            default -> null;
        };
    }
}
