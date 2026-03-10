CREATE TABLE analytics_price_candle_daily
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32) NOT NULL,
    candle_date DATE NOT NULL,
    open_price NUMERIC(19, 6) NOT NULL,
    high_price NUMERIC(19, 6) NOT NULL,
    low_price NUMERIC(19, 6) NOT NULL,
    close_price NUMERIC(19, 6) NOT NULL,
    volume NUMERIC(19, 6) NOT NULL,
    trade_count BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_analytics_price_candle_daily UNIQUE (instrument_id, candle_date)
);

CREATE INDEX idx_analytics_candle_symbol_date
    ON analytics_price_candle_daily (instrument_symbol, candle_date);

CREATE INDEX idx_analytics_candle_instrument_date
    ON analytics_price_candle_daily (instrument_id, candle_date);