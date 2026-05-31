package com.company.finance_api.portfolio.domain;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Finance DB BIST ticker'larını {@code .IS} olmadan tutar; MDS Yahoo history {@code SYMBOL.IS}
 * kullanır.
 */
public final class MdsInstrumentSymbolAliases {

  private MdsInstrumentSymbolAliases() {}

  /** Enstrüman için MDS history aramasında denenecek sembol alias listesini döner. */
  public static List<String> historyLookupSymbols(Instrument instrument) {
    if (instrument == null || instrument.getSymbol() == null) {
      return List.of();
    }
    String sym = instrument.getSymbol().trim().toUpperCase(Locale.ROOT);
    if (sym.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> out = new LinkedHashSet<>();
    out.add(sym);
    if (sym.endsWith(".IS")) {
      String base = sym.substring(0, sym.length() - 3);
      if (!base.isBlank()) {
        out.add(base);
      }
    } else if (usesYahooIsHistorySuffix(instrument)) {
      out.add(sym + ".IS");
    }
    return List.copyOf(out);
  }

  private static boolean usesYahooIsHistorySuffix(Instrument instrument) {
    Exchange exchange = instrument.getExchange();
    if (exchange == null) {
      return false;
    }
    String ex = exchange.name();
    return "BIST".equalsIgnoreCase(ex) || "YAHOO".equalsIgnoreCase(ex);
  }
}
