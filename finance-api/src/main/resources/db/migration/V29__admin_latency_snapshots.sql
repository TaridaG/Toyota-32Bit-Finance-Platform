CREATE TABLE admin_latency_snapshots (
    id BIGINT NOT NULL PRIMARY KEY,
    average_latency_ms DOUBLE PRECISION NOT NULL,
    sample_count INTEGER NOT NULL,
    measured_at TIMESTAMPTZ NOT NULL,
    detail_json TEXT,
    CONSTRAINT admin_latency_snapshots_singleton CHECK (id = 1)
);
