package com.company.marketdataservice.instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentCatalogRepository extends JpaRepository<InstrumentCatalogEntry, Long> {

    Optional<InstrumentCatalogEntry> findByInstrumentIdAndActiveTrue(Long instrumentId);
}
