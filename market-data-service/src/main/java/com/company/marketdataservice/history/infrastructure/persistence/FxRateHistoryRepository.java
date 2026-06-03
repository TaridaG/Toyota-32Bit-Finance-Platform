package com.company.marketdataservice.history.infrastructure.persistence;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * `geçmiş veri ve backfill` verisi için Spring Data JPA repository.
 */
public interface FxRateHistoryRepository extends JpaRepository<FxRateHistoryEntry, Long>, FxRateHistoryRepositoryCustom {

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

    interface DailyCloseView {
        java.time.LocalDate getDay();

        BigDecimal getPrice();

        Instant getObservedAt();
    }

    @Query(value = """
            SELECT day, price, observed_at AS observedAt
            FROM (
                SELECT DISTINCT ON (DATE(observed_at AT TIME ZONE 'UTC'))
                    DATE(observed_at AT TIME ZONE 'UTC') AS day,
                    mid AS price,
                    observed_at
                FROM mds_fx_rate_history
                WHERE canonical_symbol = :symbol
                ORDER BY DATE(observed_at AT TIME ZONE 'UTC') DESC, observed_at DESC
            ) daily
            ORDER BY day DESC
            LIMIT 2
            """, nativeQuery = true)
    List<DailyCloseView> findLastTwoDailyCloses(@Param("symbol") String symbol);

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
            select new com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto(e.observedAt, e.mid)
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

    @Query("""
            select new com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto(e.observedAt, e.mid)
            from FxRateHistoryEntry e
            where e.canonicalSymbol = :canonicalSymbol
            order by e.observedAt desc
            """)
    List<HistoryPointDto> findLatestHistoryPoint(
            @Param("canonicalSymbol") String canonicalSymbol,
            Pageable pageable
    );
}
