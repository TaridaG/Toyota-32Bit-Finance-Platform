CREATE TABLE analytics_trade_aggregate_daily
(
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_symbol VARCHAR(32),
    trade_date DATE,
    trade_count BIGINT,
    total_volume NUMERIC,
    buy_volume NUMERIC,
    sell_volume NUMERIC,
    avg_price NUMERIC,
    min_price NUMERIC,
    max_price NUMERIC
);