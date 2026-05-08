CREATE TABLE IF NOT EXISTS mds_instrument_fundamentals_cache (
    instrument_id BIGINT NOT NULL,
    canonical_symbol VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    provider_symbol VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_mds_instrument_fundamentals_cache PRIMARY KEY (instrument_id)
);

CREATE INDEX IF NOT EXISTS idx_mds_fundamentals_expires_at
    ON mds_instrument_fundamentals_cache (expires_at);
