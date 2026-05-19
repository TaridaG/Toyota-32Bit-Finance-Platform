-- TCMB EVDS: TÜFE genel endeks (TP.FG.J0) ve türetilmiş aylık / yıllık % değişim serileri.
CREATE TABLE IF NOT EXISTS mds_cpi_monthly (
    id              BIGSERIAL PRIMARY KEY,
    metric          VARCHAR(16)  NOT NULL,
    month_start     DATE         NOT NULL,
    value           NUMERIC(18, 6) NOT NULL,
    source_provider VARCHAR(32)  NOT NULL DEFAULT 'TCMB_EVDS',
    ingest_time     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_mds_cpi_monthly_metric_month UNIQUE (metric, month_start)
);

CREATE INDEX IF NOT EXISTS idx_mds_cpi_monthly_metric_month
    ON mds_cpi_monthly (metric, month_start DESC);
