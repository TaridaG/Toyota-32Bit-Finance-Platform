package com.company.finance_api.repository;

import com.company.finance_api.domain.AlarmHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** AlarmHistory entity persistence için Spring Data repository. */
public interface AlarmHistoryRepository extends JpaRepository<AlarmHistory, Long> {

  List<AlarmHistory> findByUserIdOrderByTriggeredAtDesc(UUID userId);

  Page<AlarmHistory> findByUserIdAndDeletedFalseOrderByTriggeredAtDesc(
      UUID userId, Pageable pageable);

  long countByUserIdAndDeletedFalseAndReadAtIsNull(UUID userId);

  Optional<AlarmHistory> findByIdAndUserIdAndDeletedFalse(Long id, UUID userId);
}
