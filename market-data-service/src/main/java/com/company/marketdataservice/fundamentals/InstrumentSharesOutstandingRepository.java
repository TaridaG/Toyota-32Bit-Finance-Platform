package com.company.marketdataservice.fundamentals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentSharesOutstandingRepository extends JpaRepository<InstrumentSharesOutstandingEntry, Long> {
    Optional<InstrumentSharesOutstandingEntry> findByCanonicalSymbol(String canonicalSymbol);
}

