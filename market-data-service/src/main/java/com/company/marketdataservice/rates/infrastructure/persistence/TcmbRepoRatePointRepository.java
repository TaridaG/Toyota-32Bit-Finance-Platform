package com.company.marketdataservice.rates.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TcmbRepoRatePointRepository extends JpaRepository<TcmbRepoRatePointEntity, Long> {

    Optional<TcmbRepoRatePointEntity> findByObservationDate(LocalDate observationDate);

    Optional<TcmbRepoRatePointEntity> findTopByOrderByObservationDateDesc();

    Optional<TcmbRepoRatePointEntity> findFirstByObservationDateLessThanOrderByObservationDateDesc(LocalDate observationDate);

    List<TcmbRepoRatePointEntity> findByObservationDateBetweenOrderByObservationDateAsc(
            LocalDate fromInclusive,
            LocalDate toInclusive
    );
}
