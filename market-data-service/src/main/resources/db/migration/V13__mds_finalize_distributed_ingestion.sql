ALTER TABLE mds_backfill_state
    DROP COLUMN IF EXISTS progress;

ALTER TABLE mds_backfill_state
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(128);

CREATE TABLE IF NOT EXISTS mds_backfill_chunk (
    id BIGSERIAL PRIMARY KEY,
    asset_type VARCHAR(32) NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count BIGINT NOT NULL DEFAULT 0,
    error_code VARCHAR(128),
    error_message TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE mds_backfill_chunk
    DROP CONSTRAINT IF EXISTS mds_backfill_chunk_symbol_provider_window_start_window_end_key;

ALTER TABLE mds_backfill_chunk
    DROP CONSTRAINT IF EXISTS uk_mds_backfill_chunk_asset_symbol_provider_window;

ALTER TABLE mds_backfill_chunk
    ADD CONSTRAINT uk_mds_backfill_chunk_asset_symbol_provider_window
        UNIQUE (asset_type, symbol, provider, window_start, window_end);

CREATE INDEX IF NOT EXISTS idx_mds_backfill_chunk_status_retry
    ON mds_backfill_chunk (status, next_retry_at);

CREATE INDEX IF NOT EXISTS idx_mds_backfill_chunk_asset_symbol_provider
    ON mds_backfill_chunk (asset_type, symbol, provider);
