package com.company.marketdataservice.catalog.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * `enstrüman kataloğu` verisi için Spring Data JPA repository.
 */
public interface InstrumentCatalogRepository extends JpaRepository<InstrumentCatalogEntry, Long> {

    Optional<InstrumentCatalogEntry> findByInstrumentIdAndActiveTrue(Long instrumentId);

    Optional<InstrumentCatalogEntry> findByCanonicalSymbolAndActiveTrue(String canonicalSymbol);
}
