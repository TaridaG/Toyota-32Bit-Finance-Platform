CREATE TABLE IF NOT EXISTS mds_market_price_history (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NULL,
    instrument_symbol VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    source_symbol VARCHAR(128),
    price NUMERIC(24,8) NOT NULL,
    price_type VARCHAR(64) NOT NULL,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    event_id UUID,
    ingest_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mds_market_price_history_symbol_provider_time_type
        UNIQUE (instrument_symbol, provider, observed_at, price_type),
    CONSTRAINT uk_mds_market_price_history_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_mds_market_price_history_instrument_time
    ON mds_market_price_history (instrument_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_mds_market_price_history_symbol_time
    ON mds_market_price_history (instrument_symbol, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_mds_market_price_history_provider_time
    ON mds_market_price_history (provider, observed_at DESC);

CREATE TABLE IF NOT EXISTS mds_fx_rate_history (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NULL,
    canonical_symbol VARCHAR(64) NOT NULL,
    base_currency VARCHAR(16) NOT NULL,
    quote_currency VARCHAR(16) NOT NULL,
    bid NUMERIC(24,8),
    ask NUMERIC(24,8),
    mid NUMERIC(24,8) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    event_id UUID,
    ingest_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mds_fx_rate_history_symbol_provider_time
        UNIQUE (canonical_symbol, provider, observed_at),
    CONSTRAINT uk_mds_fx_rate_history_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_mds_fx_rate_history_instrument_time
    ON mds_fx_rate_history (instrument_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_mds_fx_rate_history_symbol_time
    ON mds_fx_rate_history (canonical_symbol, observed_at DESC);

CREATE TABLE IF NOT EXISTS mds_fund_nav_history (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NULL,
    fund_code VARCHAR(64) NOT NULL,
    nav NUMERIC(24,8) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    event_id UUID,
    ingest_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mds_fund_nav_history_code_provider_time
        UNIQUE (fund_code, provider, observed_at),
    CONSTRAINT uk_mds_fund_nav_history_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_mds_fund_nav_history_instrument_time
    ON mds_fund_nav_history (instrument_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_mds_fund_nav_history_code_time
    ON mds_fund_nav_history (fund_code, observed_at DESC);
