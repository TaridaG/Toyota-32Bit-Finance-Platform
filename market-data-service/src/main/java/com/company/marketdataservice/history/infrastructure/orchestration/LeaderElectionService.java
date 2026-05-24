package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.bootstrap.config.MarketDataProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * `geçmiş veri ve backfill` infrastructure katmanı adaptörü.
 */
@Service
public class LeaderElectionService {

    private static final String LOCK_PREFIX = "INGESTION_ORCHESTRATOR";

    private final JdbcTemplate jdbcTemplate;
    private final MarketDataProperties marketDataProperties;

    public LeaderElectionService(
            JdbcTemplate jdbcTemplate,
            MarketDataProperties marketDataProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.marketDataProperties = marketDataProperties;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param env girdi parametresi
         */
    public String buildLeaderLockName(String env) {
        String resolved = (env == null || env.isBlank()) ? "dev" : env.trim().toLowerCase();
        return LOCK_PREFIX + ":" + resolved;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public String buildLeaderLockName() {
        return buildLeaderLockName(marketDataProperties.getIngestion().getEnv());
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         * @param instanceId girdi parametresi
         */
    public boolean heartbeat(String instanceId) {
        String sql = """
                UPDATE mds_orchestrator_leader_lock
                SET lock_expires_at = now() + interval '30 seconds',
                    heartbeat_at = now()
                WHERE lock_name = ?
                  AND locked_by = ?
                """;
        Integer rows = jdbcTemplate.update(sql, buildLeaderLockName(), instanceId);
        return rows != null && rows > 0;
    }
}
