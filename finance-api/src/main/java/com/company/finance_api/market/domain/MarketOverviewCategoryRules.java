package com.company.finance_api.market.domain;

import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * finance-api piyasa özet filtrelerini market-data-service katalog segment kuralları ve SPA
 * kataloğu ile hizalar.
 */
public final class MarketOverviewCategoryRules {

  /** Keep in sync with market-data-service {@code UsEquitySymbols} / {@code NasdaqRegistry}. */
  private static final Set<String> US_EQUITY_SYMBOLS =
      Set.of(
          "AAPL", "AMZN", "NVDA", "MSFT", "GOOGL", "TSLA", "META", "AVGO",
          "AMD", "NFLX", "INTC", "CSCO",
          "VOO", "VTI", "QQQ", "IVV", "SPY");
  private static final Set<String> FUND_SYMBOLS = Set.of("VOO", "VTI", "QQQ", "IVV", "SPY");
  private static final Set<String> SPOT_METAL_SYMBOLS =
      Set.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");

  private MarketOverviewCategoryRules() {}

  /**
   * Özet satırları için listeleme para birimini döner ({@code X-Currency} dönüşümü; yalnızca
   * sembol). Spot metal ({@code XAUTRY}, …) ve {@code *TRY} FX çiftleri TRY olarak kotelenir.
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

  /** Sembolden wire kategori (STOCK, FX, CRYPTO, BOND, …) çıkarır. */
  public static String inferWireCategory(String symbol) {
    if (!StringUtils.hasText(symbol)) {
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
    if (SPOT_METAL_SYMBOLS.contains(s)) {
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
   * SPA pulse segment kimliğini (bist, nasdaq, forex, metals, …) belirler; eşleşme yoksa {@code
   * null}.
   */
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
      return null;
    }
    if ("FX".equals(cat)) {
      if (SPOT_METAL_SYMBOLS.contains(s)) {
        return "metals";
      }
      return "forex";
    }
    if ("STOCK".equals(cat)) {
      if (s.endsWith(".IS")) {
        return "bist";
      }
      if (US_EQUITY_SYMBOLS.contains(s)) {
        return "nasdaq";
      }
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

  /** Sembolün UI kategori filtresine (ALL hariç) uyup uymadığını kontrol eder. */
  public static boolean matchesUiCategory(
      String symbol, String source, String exchangeName, String normalizedCategory) {
    if (!StringUtils.hasText(normalizedCategory)
        || "ALL".equalsIgnoreCase(normalizedCategory.trim())) {
      return true;
    }
    String mdsSegment = toMdsSegment(normalizedCategory);
    if (mdsSegment == null) {
      return true;
    }
    String wire = inferWireCategory(symbol);
    String pulse = pulseSegment(symbol, wire, source);
    if (pulse != null && pulse.equalsIgnoreCase(mdsSegment)) {
      return true;
    }
    // Exchange fallback is for registry-outside US/BIST equities only — not ETF, VIOP, bonds, etc.
    if (!"STOCK".equals(wire)) {
      return false;
    }
    if (!StringUtils.hasText(exchangeName)) {
      return false;
    }
    String ex = exchangeName.trim().toUpperCase(Locale.ROOT);
    return switch (mdsSegment) {
      case "bist" -> "BIST".equals(ex) || "YAHOO".equals(ex);
      case "nasdaq" -> "NASDAQ".equals(ex) || "FINNHUB".equals(ex);
      default -> false;
    };
  }

  /** UI kategori kodunu market-data-service segment koduna çevirir. */
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
      case "FUNDS", "FUND" -> "funds";
      case "BONDS", "BOND" -> "bonds";
      default -> null;
    };
  }
}
