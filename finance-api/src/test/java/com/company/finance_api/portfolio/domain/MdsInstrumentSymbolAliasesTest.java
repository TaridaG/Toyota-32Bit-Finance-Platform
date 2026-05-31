package com.company.finance_api.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import java.util.List;
import org.junit.jupiter.api.Test;

class MdsInstrumentSymbolAliasesTest {

  @Test
  void bistTickerIncludesYahooIsSuffix() {
    Instrument g = new Instrument("GARAN", "Garanti BBVA", InstrumentType.STOCK, Exchange.BIST);
    List<String> aliases = MdsInstrumentSymbolAliases.historyLookupSymbols(g);
    assertTrue(aliases.contains("GARAN"));
    assertTrue(aliases.contains("GARAN.IS"));
  }

  @Test
  void nasdaqTickerDoesNotAddIsSuffix() {
    Instrument aapl = new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    List<String> aliases = MdsInstrumentSymbolAliases.historyLookupSymbols(aapl);
    assertEquals(List.of("AAPL"), aliases);
  }
}
