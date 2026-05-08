package com.company.marketdataservice.fundamentals;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentFundamentalsCacheRepository extends JpaRepository<InstrumentFundamentalsCacheEntry, Long> {
}
