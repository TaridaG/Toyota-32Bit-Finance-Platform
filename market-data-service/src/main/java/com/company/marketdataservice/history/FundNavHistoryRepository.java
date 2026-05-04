package com.company.marketdataservice.history;

import com.company.marketdataservice.dto.HistoryPointDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FundNavHistoryRepository extends JpaRepository<FundNavHistoryEntry, Long> {

    @Query("""
            select new com.company.marketdataservice.dto.HistoryPointDto(e.observedAt, e.nav)
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
