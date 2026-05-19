package com.company.marketdataservice.rates;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MdsCpiMonthlyRepository extends JpaRepository<MdsCpiMonthlyEntity, Long> {

    Optional<MdsCpiMonthlyEntity> findTopByMetricOrderByMonthStartDesc(CpiMetric metric);

    Optional<MdsCpiMonthlyEntity> findFirstByMetricAndMonthStartLessThanOrderByMonthStartDesc(
            CpiMetric metric,
            LocalDate monthStart
    );

    List<MdsCpiMonthlyEntity> findByMetricAndMonthStartBetweenOrderByMonthStartAsc(
            CpiMetric metric,
            LocalDate fromInclusive,
            LocalDate toInclusive
    );

    long countByMetric(CpiMetric metric);

    Optional<MdsCpiMonthlyEntity> findByMetricAndMonthStart(CpiMetric metric, LocalDate monthStart);
}
