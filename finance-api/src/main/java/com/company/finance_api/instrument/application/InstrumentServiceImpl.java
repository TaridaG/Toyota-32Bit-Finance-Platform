package com.company.finance_api.instrument.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
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

  @Override
  public Instrument createInstrument(String symbol, String name, InstrumentType type, Exchange exchange) {
    Instrument instrument = new Instrument(symbol, name, type, exchange);
    return instrumentRepository.save(instrument);
  }

  @Override
  public void deactivateInstrument(Long instrumentId) {
    Instrument instrument =
        instrumentRepository
            .findById(instrumentId)
            .orElseThrow(() -> new IllegalArgumentException("Instrument not found"));
    if (!instrument.isActive()) {
      return;
    }
    instrument.deactivate();
    instrumentRepository.save(instrument);
  }
}
