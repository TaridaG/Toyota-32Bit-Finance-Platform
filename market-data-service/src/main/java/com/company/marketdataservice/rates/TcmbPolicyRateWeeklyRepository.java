package com.company.marketdataservice.rates;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TcmbPolicyRateWeeklyRepository extends JpaRepository<TcmbPolicyRateWeeklyEntity, Long> {

    Optional<TcmbPolicyRateWeeklyEntity> findByWeekStart(LocalDate weekStart);

    Optional<TcmbPolicyRateWeeklyEntity> findTopByOrderByWeekStartDesc();

    Optional<TcmbPolicyRateWeeklyEntity> findFirstByWeekStartLessThanOrderByWeekStartDesc(LocalDate weekStart);

    List<TcmbPolicyRateWeeklyEntity> findByWeekStartBetweenOrderByWeekStartAsc(LocalDate fromInclusive, LocalDate toInclusive);

    long countByWeekStartBetween(LocalDate fromInclusive, LocalDate toInclusive);
}
