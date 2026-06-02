CREATE TABLE IF NOT EXISTS mds_viop_contract_catalog (
    contract_code VARCHAR(80) PRIMARY KEY,
    underlying VARCHAR(64),
    market_type VARCHAR(16) NOT NULL,
    market_group VARCHAR(64),
    expiry_date DATE,
    settlement_type VARCHAR(32),
    currency VARCHAR(16),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    source_file VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_mds_viop_contract_expiry
    ON mds_viop_contract_catalog(expiry_date);

CREATE TABLE IF NOT EXISTS mds_viop_daily_settlement (
    trade_date DATE NOT NULL,
    contract_code VARCHAR(80) NOT NULL,
    last_price NUMERIC(18,6) NOT NULL,
    change_percent NUMERIC(18,6),
    change_amount NUMERIC(18,6),
    volume_tl NUMERIC(24,2),
    volume_qty NUMERIC(24,4),
    open_interest NUMERIC(24,4),
    source_file VARCHAR(255),
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (trade_date, contract_code)
);

CREATE INDEX IF NOT EXISTS idx_mds_viop_daily_settlement_contract
    ON mds_viop_daily_settlement(contract_code, trade_date DESC);

CREATE TABLE IF NOT EXISTS mds_viop_ingest_run (
    id BIGSERIAL PRIMARY KEY,
    run_started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    run_finished_at TIMESTAMPTZ,
    source VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    contracts_read INT NOT NULL DEFAULT 0,
    settlements_read INT NOT NULL DEFAULT 0,
    aliases_written INT NOT NULL DEFAULT 0,
    error_message TEXT
);

