ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS purchase_mode VARCHAR(16) NOT NULL DEFAULT 'NOW',
    ADD COLUMN IF NOT EXISTS acquired_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS input_mode VARCHAR(16),
    ADD COLUMN IF NOT EXISTS input_currency VARCHAR(8),
    ADD COLUMN IF NOT EXISTS input_amount NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS fx_rate_used NUMERIC(19,10),
    ADD COLUMN IF NOT EXISTS source_label VARCHAR(32);

UPDATE transactions
SET acquired_at = created_at
WHERE acquired_at IS NULL;
