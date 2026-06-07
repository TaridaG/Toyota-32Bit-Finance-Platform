package com.company.marketdataservice.viop.infrastructure.persistence;

import com.company.marketdataservice.viop.domain.ViopSettlementRow;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * VIOP günlük settlement satırları için JDBC tabanlı persistence erişimi.
 */
@Repository
@RequiredArgsConstructor
public class ViopSettlementJdbcRepository {

    private static final String UPSERT_SQL =
            """
            INSERT INTO mds_viop_daily_settlement
              (trade_date, contract_code, last_price, change_percent, change_amount, volume_tl, volume_qty, open_interest, source_file, ingested_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (trade_date, contract_code) DO UPDATE SET
              last_price = EXCLUDED.last_price,
              change_percent = EXCLUDED.change_percent,
              change_amount = EXCLUDED.change_amount,
              volume_tl = EXCLUDED.volume_tl,
              volume_qty = EXCLUDED.volume_qty,
              open_interest = EXCLUDED.open_interest,
              source_file = EXCLUDED.source_file,
              ingested_at = EXCLUDED.ingested_at
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Günlük settlement satırlarını trade date + contract code anahtarıyla toplu upsert eder.
     */
    public void upsertAll(List<ViopSettlementRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate(
                UPSERT_SQL,
                rows,
                rows.size(),
                (ps, row) -> {
                    ps.setObject(1, Date.valueOf(row.tradeDate()));
                    ps.setString(2, row.contractCode());
                    ps.setBigDecimal(3, row.lastPrice());
                    ps.setBigDecimal(4, row.changePercent());
                    ps.setBigDecimal(5, row.changeAmount());
                    ps.setBigDecimal(6, row.volumeTl());
                    ps.setBigDecimal(7, row.volumeQty());
                    ps.setBigDecimal(8, row.openInterest());
                    ps.setString(9, row.sourceFile());
                    ps.setTimestamp(10, Timestamp.from(now));
                });
    }
}

