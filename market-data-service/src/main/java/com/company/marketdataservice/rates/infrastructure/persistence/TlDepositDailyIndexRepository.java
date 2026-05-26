package com.company.marketdataservice.rates.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TlDepositDailyIndexRepository
    extends JpaRepository<TlDepositDailyIndexEntity, Long> {

  long countByMaturityCode(String maturityCode);

  Optional<TlDepositDailyIndexEntity> findTopByMaturityCodeOrderByDayDesc(String maturityCode);

  Optional<TlDepositDailyIndexEntity> findTopByMaturityCodeAndDayLessThanEqualOrderByDayDesc(
      String maturityCode, LocalDate day);

  Optional<TlDepositDailyIndexEntity> findTopByMaturityCodeOrderByDayAsc(String maturityCode);

  List<TlDepositDailyIndexEntity> findByMaturityCodeAndDayBetweenOrderByDayAsc(
      String maturityCode, LocalDate fromInclusive, LocalDate toInclusive);

  void deleteByMaturityCodeAndDayBetween(
      String maturityCode, LocalDate fromInclusive, LocalDate toInclusive);
}
