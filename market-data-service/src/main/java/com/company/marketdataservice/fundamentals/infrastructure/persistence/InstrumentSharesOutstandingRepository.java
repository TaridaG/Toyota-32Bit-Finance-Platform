package com.company.marketdataservice.fundamentals.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * `temel veri (fundamentals)` verisi için Spring Data JPA repository.
 */
public interface InstrumentSharesOutstandingRepository extends JpaRepository<InstrumentSharesOutstandingEntry, Long> {
    Optional<InstrumentSharesOutstandingEntry> findByCanonicalSymbol(String canonicalSymbol);
}

