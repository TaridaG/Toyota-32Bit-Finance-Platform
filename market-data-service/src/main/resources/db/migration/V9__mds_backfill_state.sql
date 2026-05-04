CREATE TABLE IF NOT EXISTS mds_backfill_state (
    id BIGSERIAL PRIMARY KEY,
    asset_type VARCHAR(32) NOT NULL,
    symbol VARCHAR(128) NOT NULL,
    last_fetched_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_mds_backfill_state_asset_symbol UNIQUE (asset_type, symbol)
);
