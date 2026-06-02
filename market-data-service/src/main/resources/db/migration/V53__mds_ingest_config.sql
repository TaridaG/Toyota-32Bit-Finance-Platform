CREATE TABLE IF NOT EXISTS mds_ingest_config (
    instrument_id BIGINT NOT NULL,
    segment VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    provider_override VARCHAR(64),
    provider_symbol_override VARCHAR(128),
    last_error VARCHAR(500),
    last_error_at TIMESTAMPTZ,
    CONSTRAINT pk_mds_ingest_config PRIMARY KEY (instrument_id, segment),
    CONSTRAINT fk_mds_ingest_config_instrument
        FOREIGN KEY (instrument_id) REFERENCES instruments (id)
);

CREATE INDEX IF NOT EXISTS idx_mds_ingest_config_segment_enabled
    ON mds_ingest_config (segment, enabled);

