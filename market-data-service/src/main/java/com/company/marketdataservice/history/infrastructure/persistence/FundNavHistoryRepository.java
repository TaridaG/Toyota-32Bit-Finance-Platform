package com.company.marketdataservice.history.infrastructure.persistence;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * `geçmiş veri ve backfill` verisi için Spring Data JPA repository.
 */
public interface FundNavHistoryRepository extends JpaRepository<FundNavHistoryEntry, Long> {

    Optional<FundNavHistoryEntry> findTopByFundCodeOrderByObservedAtDesc(String fundCode);

    /**
     * Latest NAV snapshot with {@code observed_at <= at} (inclusive). Used for trailing % moves on daily
     * TEFAS series where a rolling wall-clock window often contains fewer than two observations.
     */
    Optional<FundNavHistoryEntry> findTopByFundCodeAndObservedAtLessThanEqualOrderByObservedAtDesc(
            String fundCode,
            Instant observedAt
    );

    List<FundNavHistoryEntry> findByFundCodeOrderByObservedAtDesc(String fundCode, Pageable pageable);

    @Query("select e from FundNavHistoryEntry e where e.id in (select max(e2.id) from FundNavHistoryEntry e2 group by e2.fundCode)")
    List<FundNavHistoryEntry> findLatestRowPerFundCode();

    @Query("""
            select new com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto(e.observedAt, e.nav)
            from FundNavHistoryEntry e
            where e.fundCode = :fundCode
              and e.observedAt >= :fromInclusive
              and e.observedAt < :toExclusive
            order by e.observedAt asc
            """)
    List<HistoryPointDto> findHistoryPoints(
            @Param("fundCode") String fundCode,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
