CREATE TABLE admin_market_asset_snapshot (
    id BIGINT NOT NULL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    computed_at TIMESTAMPTZ,
    avg_watchlist_instruments_per_user NUMERIC(14, 4),
    avg_instruments_per_portfolio NUMERIC(14, 4),
    avg_portfolio_weight_percent NUMERIC(14, 4),
    instrument_row_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    CONSTRAINT admin_market_asset_snapshot_singleton CHECK (id = 1)
);

INSERT INTO admin_market_asset_snapshot (id, status, instrument_row_count)
VALUES (1, 'IDLE', 0);

CREATE TABLE admin_market_asset_stat (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    instrument_name VARCHAR(255) NOT NULL,
    portfolio_count INTEGER NOT NULL,
    user_count INTEGER NOT NULL,
    avg_weight_percent NUMERIC(14, 4) NOT NULL,
    sort_rank INTEGER NOT NULL,
    CONSTRAINT fk_admin_market_asset_stat_instrument
        FOREIGN KEY (instrument_id) REFERENCES instruments (id)
);

CREATE INDEX idx_admin_market_asset_stat_sort_rank ON admin_market_asset_stat (sort_rank);
