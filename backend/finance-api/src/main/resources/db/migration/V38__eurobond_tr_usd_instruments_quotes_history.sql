-- Turkey USD Eurobond module (ISIN-based). Seed uses MOCK_SEED for development / first phase.

CREATE TABLE eurobond_instruments (
    id               BIGSERIAL PRIMARY KEY,
    isin             VARCHAR(12)  NOT NULL UNIQUE,
    symbol           VARCHAR(32)  NOT NULL,
    name             VARCHAR(512) NOT NULL,
    issuer           VARCHAR(256) NOT NULL,
    currency         CHAR(3)      NOT NULL,
    maturity_date    DATE         NOT NULL,
    coupon_percent   NUMERIC(10, 4) NOT NULL,
    coupon_frequency VARCHAR(24)  NOT NULL,
    source_provider  VARCHAR(64)  NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE eurobond_quotes (
    id                        BIGSERIAL PRIMARY KEY,
    isin                      VARCHAR(12) NOT NULL REFERENCES eurobond_instruments (isin),
    clean_price               NUMERIC(14, 6),
    bid_price                 NUMERIC(14, 6),
    ask_price                 NUMERIC(14, 6),
    yield_to_maturity_percent NUMERIC(14, 6),
    daily_change_percent      NUMERIC(14, 6),
    quote_time                TIMESTAMPTZ NOT NULL,
    source_provider           VARCHAR(64) NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_eurobond_quotes_isin_time ON eurobond_quotes (isin, quote_time DESC);

CREATE TABLE eurobond_history (
    id                  BIGSERIAL PRIMARY KEY,
    isin                VARCHAR(12) NOT NULL REFERENCES eurobond_instruments (isin),
    history_date        DATE        NOT NULL,
    close_price         NUMERIC(14, 6),
    open_price          NUMERIC(14, 6),
    high_price          NUMERIC(14, 6),
    low_price           NUMERIC(14, 6),
    close_yield_percent NUMERIC(14, 6),
    open_yield_percent  NUMERIC(14, 6),
    high_yield_percent  NUMERIC(14, 6),
    low_yield_percent   NUMERIC(14, 6),
    change_percent      NUMERIC(14, 6),
    source_provider     VARCHAR(64) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_eurobond_history_isin_day UNIQUE (isin, history_date)
);

CREATE INDEX idx_eurobond_history_isin_date ON eurobond_history (isin, history_date DESC);

INSERT INTO eurobond_instruments (isin, symbol, name, issuer, currency, maturity_date, coupon_percent, coupon_frequency, source_provider, active)
VALUES
    ('US900123AL40', 'US900123AL40', 'Turkey 11.875 15-Jan-2030', 'Republic of Turkey', 'USD', '2030-01-15', 11.8750, 'SEMI_ANNUAL', 'MOCK_SEED', TRUE),
    ('US900123AT75', 'US900123AT75', 'Turkey 8.000 14-Feb-2034', 'Republic of Turkey', 'USD', '2034-02-14', 8.0000, 'SEMI_ANNUAL', 'MOCK_SEED', TRUE),
    ('US900123AY60', 'US900123AY60', 'Turkey 6.875 17-Mar-2036', 'Republic of Turkey', 'USD', '2036-03-17', 6.8750, 'SEMI_ANNUAL', 'MOCK_SEED', TRUE),
    ('US900123CM05', 'US900123CM05', 'Turkey 5.750 11-May-2047', 'Republic of Turkey', 'USD', '2047-05-11', 5.7500, 'SEMI_ANNUAL', 'MOCK_SEED', TRUE);

INSERT INTO eurobond_quotes (isin, clean_price, bid_price, ask_price, yield_to_maturity_percent, daily_change_percent, quote_time, source_provider)
VALUES
    ('US900123AL40', 102.125000, NULL, NULL, 6.120000, 0.080000, NOW() - INTERVAL '15 minutes', 'MOCK_SEED'),
    ('US900123AT75', 98.640000, NULL, NULL, 6.450000, -0.050000, NOW() - INTERVAL '20 minutes', 'MOCK_SEED'),
    ('US900123AY60', 96.210000, 96.050000, 96.370000, 6.780000, 0.120000, NOW() - INTERVAL '10 minutes', 'MOCK_SEED'),
    ('US900123CM05', 94.880000, 94.720000, 95.040000, 7.050000, -0.030000, NOW() - INTERVAL '25 minutes', 'MOCK_SEED');

INSERT INTO eurobond_history (isin, history_date, close_price, open_price, high_price, low_price, close_yield_percent, open_yield_percent, high_yield_percent, low_yield_percent, change_percent, source_provider)
SELECT e.isin,
       e.dt::date,
       ROUND((e.base_p + e.step_p * e.rn)::numeric, 4),
       ROUND((e.base_p + e.step_p * e.rn - 0.04)::numeric, 4),
       ROUND((e.base_p + e.step_p * e.rn + 0.06)::numeric, 4),
       ROUND((e.base_p + e.step_p * e.rn - 0.07)::numeric, 4),
       ROUND((e.base_y + e.step_y * e.rn)::numeric, 4),
       ROUND((e.base_y + e.step_y * e.rn + 0.02)::numeric, 4),
       ROUND((e.base_y + e.step_y * e.rn - 0.03)::numeric, 4),
       ROUND((e.base_y + e.step_y * e.rn + 0.04)::numeric, 4),
       CASE
           WHEN LAG(e.base_p + e.step_p * e.rn) OVER (PARTITION BY e.isin ORDER BY e.dt) IS NULL THEN NULL
           ELSE ROUND(
                   ((e.base_p + e.step_p * e.rn) - LAG(e.base_p + e.step_p * e.rn) OVER (PARTITION BY e.isin ORDER BY e.dt))
                   / NULLIF(LAG(e.base_p + e.step_p * e.rn) OVER (PARTITION BY e.isin ORDER BY e.dt), 0) * 100,
                   4
               )
       END,
       'MOCK_SEED'
FROM (
    SELECT u.isin,
           u.dt,
           u.base_p,
           u.step_p,
           u.base_y,
           u.step_y,
           ROW_NUMBER() OVER (PARTITION BY u.isin ORDER BY u.dt) - 1 AS rn
    FROM (
        SELECT 'US900123AL40'::varchar AS isin, gs.dt, 101.2::numeric AS base_p, 0.012::numeric AS step_p, 6.35::numeric AS base_y, -0.004::numeric AS step_y
        FROM generate_series('2024-11-01'::timestamptz, '2026-05-01'::timestamptz, interval '14 days') gs(dt)
        UNION ALL
        SELECT 'US900123AT75', gs.dt, 99.0::numeric, 0.010::numeric, 6.55::numeric, -0.003::numeric
        FROM generate_series('2024-11-01'::timestamptz, '2026-05-01'::timestamptz, interval '14 days') gs(dt)
        UNION ALL
        SELECT 'US900123AY60', gs.dt, 97.4::numeric, 0.009::numeric, 6.82::numeric, -0.003::numeric
        FROM generate_series('2024-11-01'::timestamptz, '2026-05-01'::timestamptz, interval '14 days') gs(dt)
        UNION ALL
        SELECT 'US900123CM05', gs.dt, 95.8::numeric, 0.008::numeric, 7.10::numeric, -0.002::numeric
        FROM generate_series('2024-11-01'::timestamptz, '2026-05-01'::timestamptz, interval '14 days') gs(dt)
    ) u
) e;
