package com.company.finance_api.outbox.infrastructure.persistence;

import com.company.finance_api.outbox.domain.OutboxEvent;
import com.company.finance_api.outbox.domain.enums.OutboxStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** OutboxEvent entity persistence için Spring Data repository. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        select e from OutboxEvent e
        where (e.status = :newStatus or e.status = :retryStatus)
          and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
          and e.topic not like concat(:excludedTopicPrefix, '%')
        order by e.createdAt asc
    """)
  List<OutboxEvent> lockBatch(
      OutboxStatus newStatus,
      OutboxStatus retryStatus,
      Instant now,
      String excludedTopicPrefix,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
        select e from OutboxEvent e
        where (e.status = :newStatus or e.status = :retryStatus)
          and e.topic = :topic
          and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
        order by e.createdAt asc
    """)
  List<OutboxEvent> lockBatchByTopic(
      OutboxStatus newStatus,
      OutboxStatus retryStatus,
      Instant now,
      String topic,
      Pageable pageable);
}
