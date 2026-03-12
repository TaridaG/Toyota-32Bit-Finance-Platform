CREATE TABLE analytics_trend_metric
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,

    trend_direction VARCHAR(16) NOT NULL,
    momentum NUMERIC(19,8),
    price_slope NUMERIC(19,8),

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_analytics_trend_metric UNIQUE (instrument_id, trade_date)
);

CREATE INDEX idx_analytics_trend_metric_symbol_date
    ON analytics_trend_metric (instrument_symbol, trade_date);

CREATE INDEX idx_analytics_trend_metric_instrument_date
    ON analytics_trend_metric (instrument_id, trade_date);