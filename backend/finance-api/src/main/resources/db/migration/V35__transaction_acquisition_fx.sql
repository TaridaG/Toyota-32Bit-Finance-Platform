-- Immutable TRY-hub (and USD legs) FX panel at transaction acquisition time for audit and replay.
CREATE TABLE transaction_acquisition_fx (
    transaction_id BIGINT PRIMARY KEY REFERENCES transactions (id) ON DELETE CASCADE,
    as_of TIMESTAMPTZ NOT NULL,
    usd_try NUMERIC(19, 10),
    eur_try NUMERIC(19, 10),
    gbp_try NUMERIC(19, 10),
    jpy_try NUMERIC(19, 10),
    aed_try NUMERIC(19, 10),
    eur_usd NUMERIC(19, 10),
    gbp_usd NUMERIC(19, 10),
    jpy_usd NUMERIC(19, 10)
);

CREATE INDEX idx_transaction_acquisition_fx_as_of ON transaction_acquisition_fx (as_of);
