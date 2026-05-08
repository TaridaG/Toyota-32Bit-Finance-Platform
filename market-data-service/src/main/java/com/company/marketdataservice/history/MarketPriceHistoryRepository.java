package com.company.marketdataservice.history;

import com.company.marketdataservice.dto.HistoryPointDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketPriceHistoryRepository extends JpaRepository<MarketPriceHistoryEntry, Long> {

    boolean existsByInstrumentSymbol(String instrumentSymbol);

    interface LatestMarketPriceView {
        String getSymbol();
        java.math.BigDecimal getPrice();
        String getSource();
        Instant getTimestamp();
    }

    interface DebugHistoryRowView {
        String getSymbol();
        Instant getObservedAt();
        java.math.BigDecimal getPrice();
    }

    @Query("""
            select new com.company.marketdataservice.dto.HistoryPointDto(e.observedAt, e.price)
            from MarketPriceHistoryEntry e
            where e.instrumentSymbol = :instrumentSymbol
              and e.observedAt >= :fromInclusive
              and e.observedAt < :toExclusive
            order by e.observedAt asc
            """)
    List<HistoryPointDto> findHistoryPoints(
            @Param("instrumentSymbol") String instrumentSymbol,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select new com.company.marketdataservice.dto.HistoryPointDto(e.observedAt, e.price)
            from MarketPriceHistoryEntry e
            where e.instrumentSymbol = :instrumentSymbol
            order by e.observedAt desc
            """)
    List<HistoryPointDto> findLatestHistoryPoint(
            @Param("instrumentSymbol") String instrumentSymbol,
            Pageable pageable
    );

    @Query(value = """
            SELECT price
            FROM mds_market_price_history
            WHERE instrument_symbol = :symbol
            ORDER BY observed_at DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<BigDecimal> findLatestPriceValue(@Param("symbol") String symbol);

    @Query(value = """
            SELECT DISTINCT ON (instrument_symbol)
                instrument_symbol AS symbol,
                price AS price,
                provider AS source,
                observed_at AS timestamp
            FROM mds_market_price_history
            ORDER BY instrument_symbol, observed_at DESC, id DESC
            """, nativeQuery = true)
    List<LatestMarketPriceView> findLatestPricesPerSymbol();

    @Query(value = """
            SELECT instrument_symbol AS symbol, observed_at AS observedAt, price AS price
            FROM mds_market_price_history
            ORDER BY observed_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<DebugHistoryRowView> findLatestDebugRows(@Param("limit") int limit);

    @Query(value = """
            SELECT instrument_symbol AS symbol, observed_at AS observedAt, price AS price
            FROM mds_market_price_history
            WHERE instrument_symbol = :symbol
            ORDER BY observed_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<DebugHistoryRowView> findLatestDebugRowsBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(DISTINCT DATE(observed_at))
            FROM mds_market_price_history
            WHERE instrument_symbol = :symbol
            """, nativeQuery = true)
    long countDistinctDaysBySymbol(@Param("symbol") String symbol);

    @Query(value = """
            SELECT COUNT(DISTINCT DATE(observed_at))
            FROM mds_market_price_history
            WHERE instrument_symbol = :symbol
              AND observed_at >= :fromInclusive
            """, nativeQuery = true)
    long countDistinctDaysBySymbolSince(
            @Param("symbol") String symbol,
            @Param("fromInclusive") Instant fromInclusive
    );
}
