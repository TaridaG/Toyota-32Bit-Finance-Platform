package com.company.marketdataservice.catalog.domain;
import java.util.Locale;
import java.util.Set;


/**
 * Frontend Markets tablosu ile uyumlu sembol segment ve wire category kurallarını uygular.
 */
public final class MarketCatalogSegmentRules {

    private static final Set<String> FUND_SYMBOLS = Set.of("VOO", "VTI", "QQQ", "IVV", "SPY");
    private static final Set<String> SPOT_METAL_SYMBOLS =
            Set.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");
    private static final Set<String> METAL_SYMBOLS = SPOT_METAL_SYMBOLS;

    private MarketCatalogSegmentRules() {}

    /**
     * Sembol için kabaca wire category döner (CRYPTO, STOCK, FX, FUND, METAL, BOND); SPA katalog birleşmesi ile uyumludur.
     *
     * @param symbol enstrüman sembolü
     * @return wire category
     */
    public static String inferWireCategory(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "STOCK";
        }
        String s = symbol.trim().toUpperCase(Locale.ROOT);
        if (s.startsWith("VIOP_")) {
            return "DERIVATIVE";
        }
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
     * UI pulse segment kimliğini döner ({@code MarketCategory} içindeki {@code all} hariç); kapsam dışındaysa {@code null}.
     *
     * @param symbol enstrüman sembolü
     * @param wireCategory wire category
     * @param source fiyat kaynağı (provider)
     * @return pulse segment id veya {@code null}
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
            return null;
        }
        if ("FX".equals(cat)) {
            if (SPOT_METAL_SYMBOLS.contains(s)) {
                return "metals";
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
        if ("DERIVATIVE".equals(cat)) {
            return null;
        }
        return null;
    }

    /**
     * FX history path kullanılıp kullanılmadığını belirler.
     *
     * @param symbol enstrüman sembolü
     * @param wireCategory wire category
     * @return FX history path kullanılıyorsa {@code true}
     */
    public static boolean usesFxHistoryPath(String symbol, String wireCategory) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        String s = symbol.trim().toUpperCase(Locale.ROOT);
        String cat = wireCategory == null ? "" : wireCategory.trim().toUpperCase(Locale.ROOT);
        return "FX".equals(cat) || SPOT_METAL_SYMBOLS.contains(s);
    }
}
