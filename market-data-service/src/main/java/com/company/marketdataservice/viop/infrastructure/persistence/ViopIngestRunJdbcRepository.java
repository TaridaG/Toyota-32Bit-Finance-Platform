package com.company.marketdataservice.viop.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * VIOP ingest çalıştırma (run) kayıtları için JDBC tabanlı persistence erişimi.
 */
@Repository
@RequiredArgsConstructor
public class ViopIngestRunJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Yeni bir ingest run kaydı açar ve oluşan run kimliğini döner.
     */
    public long start(String source) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(
                                    "INSERT INTO mds_viop_ingest_run (run_started_at, source, status) VALUES (?, ?, ?)",
                                    new String[] {"id"});
                    ps.setTimestamp(1, Timestamp.from(Instant.now()));
                    ps.setString(2, source);
                    ps.setString(3, "RUNNING");
                    return ps;
                },
                keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    public void finishSuccess(long runId, int contractsRead, int settlementsRead, int aliasesWritten) {
        if (runId <= 0L) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE mds_viop_ingest_run
                   SET run_finished_at = ?, status = ?, contracts_read = ?, settlements_read = ?, aliases_written = ?, error_message = NULL
                 WHERE id = ?
                """,
                Timestamp.from(Instant.now()),
                "SUCCESS",
                contractsRead,
                settlementsRead,
                aliasesWritten,
                runId);
    }

    public void finishFailure(long runId, String message) {
        if (runId <= 0L) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE mds_viop_ingest_run
                   SET run_finished_at = ?, status = ?, error_message = ?
                 WHERE id = ?
                """,
                Timestamp.from(Instant.now()),
                "FAILED",
                message,
                runId);
    }
}

