package com.company.finance_api.portfolio;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.InstrumentType;
import java.util.Locale;
import java.util.Set;

/**
 * Bir enstrümanın transaction {@code price} ve {@code totalAmount} alanları için listeleme/kotasyon
 * para birimi. Trade execution ve portfolio valuation akışlarıyla uyumlu kalmalıdır.
 */
public final class InstrumentListingCurrency {

  /** BIST/Yahoo *.IS ticker'ları; DB'de exchange kayması olsa bile TRY olarak çözülür. */
  private static final Set<String> KNOWN_TRY_LISTED_STOCKS =
      Set.of(
          "GARAN", "ASELS", "THYAO", "AKBNK", "YKBNK", "ISCTR", "EKGYO", "KCHOL", "TUPRS", "SAHOL");

  private InstrumentListingCurrency() {}

  /** Enstrüman için listeleme para birimini (TRY, EUR, USD) çözer. */
  public static String resolve(Instrument instrument) {
    if (instrument.getExchange() != null) {
      String ex = instrument.getExchange().name();
      if ("BIST".equalsIgnoreCase(ex)
          || "YAHOO".equalsIgnoreCase(ex)
          || "TEFAS".equalsIgnoreCase(ex)) {
        return "TRY";
      }
    }
    String symbol =
        instrument.getSymbol() == null ? "" : instrument.getSymbol().toUpperCase(Locale.ROOT);
    if (symbol.endsWith("TRY")) {
      return "TRY";
    }
    if (symbol.endsWith("EUR")) {
      return "EUR";
    }
    if (instrument.getType() == InstrumentType.STOCK
        && !symbol.isEmpty()
        && KNOWN_TRY_LISTED_STOCKS.contains(symbol)) {
      return "TRY";
    }
    return "USD";
  }
}
