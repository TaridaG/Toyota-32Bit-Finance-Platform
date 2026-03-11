CREATE TABLE analytics_vwap_daily
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,

    total_volume NUMERIC(19,8) NOT NULL,
    total_price_volume NUMERIC(19,8) NOT NULL,
    vwap NUMERIC(19,8) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_analytics_vwap_daily
        UNIQUE (instrument_id, trade_date)
);

CREATE INDEX idx_vwap_symbol_date
    ON analytics_vwap_daily (instrument_symbol, trade_date);