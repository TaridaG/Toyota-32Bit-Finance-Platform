package com.company.finance_api.instrument.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import java.util.List;

/** InstrumentService iş mantığını uygular (instrument service). */
public interface InstrumentService {

  /** Aktif tüm enstrümanları listeler. */
  List<Instrument> getAllActive();

  List<Instrument> getByType(InstrumentType type);

  Instrument createInstrument(String symbol, String name, InstrumentType type, com.company.finance_api.instrument.domain.enums.Exchange exchange);

  void deactivateInstrument(Long instrumentId);
}
