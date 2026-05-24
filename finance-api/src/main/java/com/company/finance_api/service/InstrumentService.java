package com.company.finance_api.service;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.InstrumentType;
import java.util.List;

/** InstrumentService iş mantığını uygular (instrument service). */
public interface InstrumentService {

  /** getAllActive sözleşmesi. */
  List<Instrument> getAllActive();

  List<Instrument> getByType(InstrumentType type);
}
