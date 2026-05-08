CREATE TABLE IF NOT EXISTS mds_instrument_shares_outstanding (
    instrument_id BIGINT NOT NULL,
    canonical_symbol VARCHAR(64) NOT NULL,
    shares_outstanding NUMERIC(28,4) NOT NULL,
    source VARCHAR(64) NOT NULL,
    provider_symbol VARCHAR(128),
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_mds_instrument_shares_outstanding PRIMARY KEY (instrument_id),
    CONSTRAINT uk_mds_instrument_shares_symbol UNIQUE (canonical_symbol)
);

CREATE INDEX IF NOT EXISTS idx_mds_instrument_shares_verified_at
    ON mds_instrument_shares_outstanding (verified_at);
