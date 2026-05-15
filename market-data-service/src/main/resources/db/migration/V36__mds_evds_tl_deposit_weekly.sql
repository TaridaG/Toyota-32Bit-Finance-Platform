-- TCMB EVDS: weighted average TL deposit rates (stock), by maturity bucket; weekly rows derived from EVDS observations.
CREATE TABLE IF NOT EXISTS mds_evds_tl_deposit_weekly (
    id BIGSERIAL PRIMARY KEY,
    week_start DATE NOT NULL,
    maturity_code VARCHAR(16) NOT NULL,
    rate_percent NUMERIC(12, 4) NOT NULL,
    source_observation_date DATE,
    source_provider VARCHAR(32) NOT NULL DEFAULT 'TCMB_EVDS',
    ingest_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mds_tl_dep_week_maturity UNIQUE (week_start, maturity_code)
);

CREATE INDEX IF NOT EXISTS idx_mds_tl_dep_maturity_week_desc
    ON mds_evds_tl_deposit_weekly (maturity_code, week_start DESC);
