package com.company.marketdataservice.history.infrastructure.persistence;

import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Repository
public class FxRateHistoryRepositoryImpl implements FxRateHistoryRepositoryCustom {

    private static final String SPOT_METAL_HISTORY_SQL = """
            SELECT picked.observed_at, picked.mid
            FROM (
                SELECT DISTINCT ON (DATE(observed_at AT TIME ZONE 'UTC'))
                    observed_at,
                    mid
                FROM mds_fx_rate_history
                WHERE canonical_symbol = ?
                  AND observed_at >= ?
                  AND observed_at < ?
                ORDER BY DATE(observed_at AT TIME ZONE 'UTC') ASC,
                    CASE provider
                        WHEN 'MINTED_METAL_LBMA' THEN 1
                        WHEN 'STOOQ_SPOT' THEN 2
                        WHEN 'YAHOO_DERIVED_SPOT' THEN 3
                        WHEN 'TCMB_ARCHIVE' THEN 4
                        WHEN 'EVDS' THEN 5
                        WHEN 'TCMB' THEN 6
                        ELSE 99
                    END ASC,
                    observed_at DESC,
                    id DESC
            ) picked
            ORDER BY picked.observed_at ASC
            """;

    private static final String SPOT_METAL_LAST_TWO_DAILY_SQL = """
            SELECT day, price, observed_at AS observedAt
            FROM (
                SELECT DISTINCT ON (DATE(observed_at AT TIME ZONE 'UTC'))
                    DATE(observed_at AT TIME ZONE 'UTC') AS day,
                    mid AS price,
                    observed_at
                FROM mds_fx_rate_history
                WHERE canonical_symbol = ?
                ORDER BY DATE(observed_at AT TIME ZONE 'UTC') DESC,
                    CASE provider
                        WHEN 'MINTED_METAL_LBMA' THEN 1
                        WHEN 'STOOQ_SPOT' THEN 2
                        WHEN 'YAHOO_DERIVED_SPOT' THEN 3
                        WHEN 'TCMB_ARCHIVE' THEN 4
                        WHEN 'EVDS' THEN 5
                        WHEN 'TCMB' THEN 6
                        ELSE 99
                    END ASC,
                    observed_at DESC,
                    id DESC
            ) daily
            ORDER BY day DESC
            LIMIT 2
            """;

    private final JdbcTemplate jdbcTemplate;

    public FxRateHistoryRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<HistoryPointDto> findSpotMetalHistoryPoints(
            String canonicalSymbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        String symbol = canonicalSymbol.trim().toUpperCase(Locale.ROOT);
        return jdbcTemplate.query(
                SPOT_METAL_HISTORY_SQL,
                (rs, rowNum) -> new HistoryPointDto(
                        rs.getTimestamp("observed_at").toInstant(),
                        rs.getBigDecimal("mid")
                ),
                symbol,
                Timestamp.from(fromInclusive),
                Timestamp.from(toExclusive)
        );
    }

    @Override
    public int deleteBySymbolProviderAndObservedAtBetween(
            String canonicalSymbol,
            String provider,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return jdbcTemplate.update(
                """
                        DELETE FROM mds_fx_rate_history
                        WHERE canonical_symbol = ?
                          AND provider = ?
                          AND observed_at >= ?
                          AND observed_at < ?
                        """,
                canonicalSymbol.trim().toUpperCase(Locale.ROOT),
                provider,
                Timestamp.from(fromInclusive),
                Timestamp.from(toExclusive)
        );
    }

    private static final RowMapper<FxRateHistoryRepository.DailyCloseView> DAILY_CLOSE_ROW_MAPPER = (rs, rowNum) -> {
        Date day = rs.getDate("day");
        java.math.BigDecimal price = rs.getBigDecimal("price");
        Timestamp ts = rs.getTimestamp("observedAt");
        java.time.LocalDate localDay = day == null ? null : day.toLocalDate();
        Instant observedAt = ts == null ? null : ts.toInstant();
        return new FxRateHistoryRepository.DailyCloseView() {
            @Override
            public java.time.LocalDate getDay() {
                return localDay;
            }

            @Override
            public java.math.BigDecimal getPrice() {
                return price;
            }

            @Override
            public Instant getObservedAt() {
                return observedAt;
            }
        };
    };

    @Override
    public List<FxRateHistoryRepository.DailyCloseView> findSpotMetalLastTwoDailyCloses(String canonicalSymbol) {
        String symbol = canonicalSymbol.trim().toUpperCase(Locale.ROOT);
        return jdbcTemplate.query(SPOT_METAL_LAST_TWO_DAILY_SQL, DAILY_CLOSE_ROW_MAPPER, symbol);
    }
}
