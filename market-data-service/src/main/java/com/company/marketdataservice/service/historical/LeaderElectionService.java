package com.company.marketdataservice.service.historical;

import com.company.marketdataservice.config.MarketDataProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    public String buildLeaderLockName(String env) {
        String resolved = (env == null || env.isBlank()) ? "dev" : env.trim().toLowerCase();
        return LOCK_PREFIX + ":" + resolved;
    }

    public String buildLeaderLockName() {
        return buildLeaderLockName(marketDataProperties.getIngestion().getEnv());
    }

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
