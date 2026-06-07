package com.company.finance_api.portfolio.domain;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import java.util.Locale;
import java.util.Optional;

/**
 * TL mevduat enstrümanlarını ({@code TLDEP_*} sembol prefix'i ve {@code DEPOSIT} tipi) tanır ve
 * vade kodunu çözer.
 */
public final class TlDepositInstruments {

  public static final String SYMBOL_PREFIX = "TLDEP_";

  private TlDepositInstruments() {}

  public static boolean isTlDeposit(Instrument instrument) {
    if (instrument == null) {
      return false;
    }
    return instrument.getType() == InstrumentType.DEPOSIT
        || resolveMaturityCode(instrument.getSymbol()).isPresent();
  }

  public static Optional<String> resolveMaturityCode(Instrument instrument) {
    return instrument == null ? Optional.empty() : resolveMaturityCode(instrument.getSymbol());
  }

  public static Optional<String> resolveMaturityCode(String symbol) {
    String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    if (!normalized.startsWith(SYMBOL_PREFIX) || normalized.length() <= SYMBOL_PREFIX.length()) {
      return Optional.empty();
    }
    return Optional.of(normalized.substring(SYMBOL_PREFIX.length()));
  }
}
