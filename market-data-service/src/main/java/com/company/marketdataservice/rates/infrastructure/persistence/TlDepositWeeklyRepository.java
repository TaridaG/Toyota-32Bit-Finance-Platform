package com.company.marketdataservice.rates.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * `makro oran` verisi için Spring Data JPA repository.
 */
public interface TlDepositWeeklyRepository extends JpaRepository<TlDepositWeeklyEntity, Long> {

    Optional<TlDepositWeeklyEntity> findByWeekStartAndMaturityCode(LocalDate weekStart, String maturityCode);

    Optional<TlDepositWeeklyEntity> findTopByMaturityCodeOrderByWeekStartDesc(String maturityCode);

    Optional<TlDepositWeeklyEntity> findFirstByMaturityCodeAndWeekStartLessThanOrderByWeekStartDesc(
            String maturityCode,
            LocalDate weekStart
    );

    List<TlDepositWeeklyEntity> findByMaturityCodeAndWeekStartBetweenOrderByWeekStartAsc(
            String maturityCode,
            LocalDate fromInclusive,
            LocalDate toInclusive
    );

    long countByMaturityCode(String maturityCode);
}
