package com.company.marketdataservice.history.infrastructure.persistence;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * `geçmiş veri ve backfill` verisi için Spring Data JPA repository.
 */
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
            select new com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto(e.observedAt, e.price)
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
            select new com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto(e.observedAt, e.price)
            from MarketPriceHistoryEntry e
            where e.instrumentSymbol = :instrumentSymbol
              and e.priceType = :priceType
              and e.observedAt >= :fromInclusive
              and e.observedAt < :toExclusive
            order by e.observedAt asc
            """)
    List<HistoryPointDto> findHistoryPointsByPriceType(
            @Param("instrumentSymbol") String instrumentSymbol,
            @Param("priceType") String priceType,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select new com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto(e.observedAt, e.price)
            from MarketPriceHistoryEntry e
            where e.instrumentSymbol = :instrumentSymbol
            order by e.observedAt desc
            """)
    List<HistoryPointDto> findLatestHistoryPoint(
            @Param("instrumentSymbol") String instrumentSymbol,
            Pageable pageable
    );

    @Query("""
            select e.price
            from MarketPriceHistoryEntry e
            where e.instrumentSymbol = :instrumentSymbol
              and e.provider = :provider
              and e.priceType = :priceType
              and e.observedAt < :beforeExclusive
            order by e.observedAt desc
            """)
    List<BigDecimal> findLatestPricesBefore(
            @Param("instrumentSymbol") String instrumentSymbol,
            @Param("provider") String provider,
            @Param("priceType") String priceType,
            @Param("beforeExclusive") Instant beforeExclusive,
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

    /** Latest row per TCMB Hazine yield symbol (TRBOND*), for catalog merge when snapshot bus has no bond ticks yet. */
    @Query(value = """
            SELECT DISTINCT ON (instrument_symbol)
                instrument_symbol AS symbol,
                price AS price,
                provider AS source,
                observed_at AS timestamp
            FROM mds_market_price_history
            WHERE instrument_symbol LIKE 'TRBOND%'
            ORDER BY instrument_symbol, observed_at DESC, id DESC
            """, nativeQuery = true)
    List<LatestMarketPriceView> findLatestTrbondPricesPerSymbol();

    /** Latest row per *USDT crypto pair when live snapshot has no tick (Binance/CoinGecko outage). */
    @Query(value = """
            SELECT DISTINCT ON (instrument_symbol)
                instrument_symbol AS symbol,
                price AS price,
                provider AS source,
                observed_at AS timestamp
            FROM mds_market_price_history
            WHERE instrument_symbol LIKE '%USDT'
            ORDER BY instrument_symbol, observed_at DESC, id DESC
            """, nativeQuery = true)
    List<LatestMarketPriceView> findLatestCryptoPricesPerSymbol();

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
                    price,
                    observed_at
                FROM mds_market_price_history
                WHERE instrument_symbol = :symbol
                ORDER BY DATE(observed_at AT TIME ZONE 'UTC') DESC, observed_at DESC
            ) daily
            ORDER BY day DESC
            LIMIT 2
            """, nativeQuery = true)
    List<DailyCloseView> findLastTwoDailyCloses(@Param("symbol") String symbol);

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
