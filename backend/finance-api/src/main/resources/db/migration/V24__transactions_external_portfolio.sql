ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS external_portfolio_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_transactions_external_portfolio'
          AND table_name = 'transactions'
    ) THEN
        ALTER TABLE transactions
            ADD CONSTRAINT fk_transactions_external_portfolio
                FOREIGN KEY (external_portfolio_id)
                    REFERENCES external_portfolios (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_transactions_external_portfolio_id
    ON transactions (external_portfolio_id);

