CREATE TABLE IF NOT EXISTS mds_tcmb_policy_rate_weekly (
    id BIGSERIAL PRIMARY KEY,
    week_start DATE NOT NULL,
    rate_percent NUMERIC(12, 4) NOT NULL,
    source_provider VARCHAR(32) NOT NULL DEFAULT 'TCMB_EVDS',
    ingest_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mds_tcmb_policy_rate_week_week UNIQUE (week_start)
);

CREATE INDEX IF NOT EXISTS idx_mds_tcmb_policy_rate_week_start_desc
    ON mds_tcmb_policy_rate_weekly (week_start DESC);
