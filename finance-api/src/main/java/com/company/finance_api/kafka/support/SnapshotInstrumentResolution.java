package com.company.finance_api.kafka.support;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.kafka.event.FundSnapshotUpdatedEvent;
import com.company.finance_api.kafka.event.FxSnapshotUpdatedEvent;
import com.company.finance_api.repository.InstrumentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Snapshot mesajındaki sembolü {@link com.company.finance_api.domain.Instrument} kaydına çözer. */
@Component
@RequiredArgsConstructor
public class SnapshotInstrumentResolution {

  private final InstrumentRepository instrumentRepository;

  public Optional<Instrument> resolveFx(FxSnapshotUpdatedEvent event) {
    if (event.instrumentId() != null) {
      Optional<Instrument> byId = instrumentRepository.findById(event.instrumentId());
      if (byId.isPresent() && byId.get().isActive()) {
        return byId;
      }
    }
    if (event.canonicalSymbol() != null && !event.canonicalSymbol().isBlank()) {
      return instrumentRepository
          .findBySymbol(event.canonicalSymbol().trim())
          .filter(Instrument::isActive);
    }
    return Optional.empty();
  }

  public Optional<Instrument> resolveFund(FundSnapshotUpdatedEvent event) {
    if (event.instrumentId() != null) {
      Optional<Instrument> byId = instrumentRepository.findById(event.instrumentId());
      if (byId.isPresent() && byId.get().isActive()) {
        return byId;
      }
    }
    if (event.fundCode() != null && !event.fundCode().isBlank()) {
      String code = event.fundCode().trim();
      Optional<Instrument> prefixed =
          instrumentRepository
              .findBySymbol("FUND_" + code.toUpperCase())
              .filter(Instrument::isActive);
      if (prefixed.isPresent()) {
        return prefixed;
      }
      return instrumentRepository.findBySymbol(code.toUpperCase()).filter(Instrument::isActive);
    }
    return Optional.empty();
  }
}
