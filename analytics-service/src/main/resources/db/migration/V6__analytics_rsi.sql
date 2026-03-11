CREATE TABLE analytics_rsi
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,

    rsi14 NUMERIC(19,8),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_rsi_symbol_date
        UNIQUE (instrument_id, trade_date)
);

CREATE INDEX idx_rsi_symbol_date
    ON analytics_rsi (instrument_symbol, trade_date);