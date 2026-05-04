package com.company.marketdataservice.history;

import com.company.marketdataservice.dto.HistoryPointDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FxRateHistoryRepository extends JpaRepository<FxRateHistoryEntry, Long> {

    interface LatestFxRateView {
        String getCanonicalSymbol();
        java.math.BigDecimal getBid();
        java.math.BigDecimal getAsk();
        java.math.BigDecimal getMid();
        String getSource();
        Instant getObservedAt();
    }

    @Query(value = """
            SELECT DISTINCT ON (canonical_symbol)
                canonical_symbol AS canonicalSymbol,
                bid AS bid,
                ask AS ask,
                mid AS mid,
                provider AS source,
                observed_at AS observedAt
            FROM mds_fx_rate_history
            ORDER BY canonical_symbol, observed_at DESC, id DESC
            """, nativeQuery = true)
    List<LatestFxRateView> findLatestRatesPerSymbol();

    @Query(value = """
            SELECT COUNT(DISTINCT DATE(observed_at))
            FROM mds_fx_rate_history
            WHERE canonical_symbol = :canonicalSymbol
            """, nativeQuery = true)
    long countDistinctDaysBySymbol(@Param("canonicalSymbol") String canonicalSymbol);

    @Query(value = """
            SELECT COUNT(DISTINCT DATE(observed_at))
            FROM mds_fx_rate_history
            WHERE canonical_symbol = :canonicalSymbol
              AND observed_at >= :fromInclusive
            """, nativeQuery = true)
    long countDistinctDaysBySymbolSince(
            @Param("canonicalSymbol") String canonicalSymbol,
            @Param("fromInclusive") Instant fromInclusive
    );

    @Query("""
            select new com.company.marketdataservice.dto.HistoryPointDto(e.observedAt, e.mid)
            from FxRateHistoryEntry e
            where e.canonicalSymbol = :canonicalSymbol
              and e.observedAt >= :fromInclusive
              and e.observedAt < :toExclusive
            order by e.observedAt asc
            """)
    List<HistoryPointDto> findHistoryPoints(
            @Param("canonicalSymbol") String canonicalSymbol,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
