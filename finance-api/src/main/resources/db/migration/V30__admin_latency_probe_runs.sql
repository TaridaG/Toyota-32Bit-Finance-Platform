-- History of probe averages (one row per refresh); line-item samples only for the latest run.

CREATE TABLE admin_latency_probe_run (
    id BIGSERIAL PRIMARY KEY,
    average_latency_ms DOUBLE PRECISION NOT NULL,
    sample_count INTEGER NOT NULL,
    measured_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE admin_latency_probe_sample (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES admin_latency_probe_run (id) ON DELETE CASCADE,
    path VARCHAR(512) NOT NULL,
    duration_ms DOUBLE PRECISION NOT NULL,
    sort_order INTEGER NOT NULL
);

CREATE INDEX idx_admin_latency_probe_sample_run_id ON admin_latency_probe_sample (run_id);

INSERT INTO admin_latency_probe_run (average_latency_ms, sample_count, measured_at)
SELECT average_latency_ms, sample_count, measured_at
FROM admin_latency_snapshots
WHERE id = 1;

DROP TABLE admin_latency_snapshots;
