package com.company.finance_api.repository;

import com.company.finance_api.domain.EurobondHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** EurobondHistory entity persistence için Spring Data repository. */
public interface EurobondHistoryRepository extends JpaRepository<EurobondHistory, Long> {

  List<EurobondHistory> findByIsinAndHistoryDateGreaterThanEqualOrderByHistoryDateAsc(
      String isin, LocalDate from);

  boolean existsByIsinAndHistoryDate(String isin, LocalDate historyDate);

  Optional<EurobondHistory> findByIsinAndHistoryDate(String isin, LocalDate historyDate);

  long countByIsin(String isin);

  List<EurobondHistory> findTop2ByIsinOrderByHistoryDateDesc(String isin);

  Optional<EurobondHistory> findFirstByIsinAndHistoryDateLessThanOrderByHistoryDateDesc(
      String isin, LocalDate historyDate);
}
