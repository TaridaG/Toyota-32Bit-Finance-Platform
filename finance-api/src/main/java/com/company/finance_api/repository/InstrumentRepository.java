package com.company.finance_api.repository;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.InstrumentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Instrument entity persistence için Spring Data repository. */
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

  long countByActiveTrue();

  List<Instrument> findByActiveTrue();

  List<Instrument> findByTypeAndActiveTrue(InstrumentType type);

  Optional<Instrument> findBySymbol(String symbol);
}
