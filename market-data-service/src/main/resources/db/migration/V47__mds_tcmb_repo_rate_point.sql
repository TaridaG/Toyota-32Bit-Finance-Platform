CREATE TABLE IF NOT EXISTS mds_tcmb_repo_rate_point (
    id               BIGSERIAL PRIMARY KEY,
    observation_date DATE           NOT NULL,
    rate_percent     NUMERIC(12, 4) NOT NULL,
    source_provider  VARCHAR(32)    NOT NULL DEFAULT 'TCMB_EVDS',
    ingest_time      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mds_tcmb_repo_rate_observation UNIQUE (observation_date)
);

CREATE INDEX IF NOT EXISTS idx_mds_tcmb_repo_rate_observation_desc
    ON mds_tcmb_repo_rate_point (observation_date DESC);
