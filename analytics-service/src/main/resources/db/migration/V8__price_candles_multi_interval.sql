CREATE TABLE price_candles
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32) NOT NULL,
    candle_interval VARCHAR(32) NOT NULL,
    open_time TIMESTAMP NOT NULL,

    open_price NUMERIC(19, 6) NOT NULL,
    high_price NUMERIC(19, 6) NOT NULL,
    low_price NUMERIC(19, 6) NOT NULL,
    close_price NUMERIC(19, 6) NOT NULL,

    volume NUMERIC(19, 6) NOT NULL,
    trade_count BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_price_candle_bucket UNIQUE (instrument_id, candle_interval, open_time)
);

CREATE INDEX idx_price_candles_symbol_interval_time
    ON price_candles (instrument_symbol, candle_interval, open_time);

CREATE INDEX idx_price_candles_instrument_interval_time
    ON price_candles (instrument_id, candle_interval, open_time);