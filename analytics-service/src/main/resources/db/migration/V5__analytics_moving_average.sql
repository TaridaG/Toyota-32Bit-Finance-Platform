CREATE TABLE analytics_moving_average
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,

    ma7 NUMERIC(19,8),
    ma30 NUMERIC(19,8),
    ma90 NUMERIC(19,8),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_ma_symbol_date
        UNIQUE (instrument_id, trade_date)
);

CREATE INDEX idx_ma_symbol_date
    ON analytics_moving_average (instrument_symbol, trade_date);