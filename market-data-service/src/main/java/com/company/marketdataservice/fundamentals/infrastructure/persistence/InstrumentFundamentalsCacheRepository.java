package com.company.marketdataservice.fundamentals.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * `temel veri (fundamentals)` verisi için Spring Data JPA repository.
 */
public interface InstrumentFundamentalsCacheRepository extends JpaRepository<InstrumentFundamentalsCacheEntry, Long> {
}
