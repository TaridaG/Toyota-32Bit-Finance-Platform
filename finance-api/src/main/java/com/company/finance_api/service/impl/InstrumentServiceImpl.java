package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.service.InstrumentService;
import java.util.List;
import org.springframework.stereotype.Service;

/** InstrumentServiceImpl iş mantığını uygular (instrument service). */
@Service
public class InstrumentServiceImpl implements InstrumentService {

  private final InstrumentRepository instrumentRepository;

  public InstrumentServiceImpl(InstrumentRepository instrumentRepository) {
    this.instrumentRepository = instrumentRepository;
  }

  /** AllActive sorgusunu döner. */
  @Override
  public List<Instrument> getAllActive() {
    return instrumentRepository.findByActiveTrue();
  }

  /** ByType sorgusunu döner. */
  @Override
  public List<Instrument> getByType(InstrumentType type) {
    return instrumentRepository.findByTypeAndActiveTrue(type);
  }
}
