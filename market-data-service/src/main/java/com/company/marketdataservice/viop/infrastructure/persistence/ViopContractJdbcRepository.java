package com.company.marketdataservice.viop.infrastructure.persistence;

import com.company.marketdataservice.viop.domain.ViopContractRow;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * VIOP sözleşme kataloğu satırları için JDBC tabanlı persistence erişimi.
 */
@Repository
@RequiredArgsConstructor
public class ViopContractJdbcRepository {

    private static final String UPSERT_SQL =
            """
            INSERT INTO mds_viop_contract_catalog
              (contract_code, underlying, market_type, market_group, expiry_date, settlement_type, currency, pazar, is_active, source_file, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)
            ON CONFLICT (contract_code) DO UPDATE SET
              underlying = EXCLUDED.underlying,
              market_type = EXCLUDED.market_type,
              market_group = EXCLUDED.market_group,
              expiry_date = EXCLUDED.expiry_date,
              settlement_type = EXCLUDED.settlement_type,
              currency = EXCLUDED.currency,
              pazar = EXCLUDED.pazar,
              is_active = TRUE,
              source_file = EXCLUDED.source_file,
              updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Sözleşme satırlarını toplu upsert eder ve kaynak dosya bilgisini günceller.
     */
    public void upsertAll(List<ViopContractRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate(
                UPSERT_SQL,
                rows,
                rows.size(),
                (ps, row) -> {
                    ps.setString(1, row.contractCode());
                    ps.setString(2, row.underlying());
                    ps.setString(3, row.marketType());
                    ps.setString(4, row.marketGroup());
                    ps.setObject(5, row.expiryDate() == null ? null : Date.valueOf(row.expiryDate()));
                    ps.setString(6, row.settlementType());
                    ps.setString(7, row.currency());
                    ps.setString(8, row.pazar());
                    ps.setString(9, row.sourceFile());
                    ps.setTimestamp(10, Timestamp.from(now));
                });
    }

    public void markMissingAsInactive(List<String> activeContractCodes) {
        if (activeContractCodes == null || activeContractCodes.isEmpty()) {
            jdbcTemplate.update("UPDATE mds_viop_contract_catalog SET is_active = FALSE");
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(activeContractCodes.size(), "?"));
        String sql = "UPDATE mds_viop_contract_catalog SET is_active = FALSE WHERE contract_code NOT IN (" + placeholders + ")";
        jdbcTemplate.update(sql, activeContractCodes.toArray());
    }
}

