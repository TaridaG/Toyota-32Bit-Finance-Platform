ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_deletion_requested_at
    ON users (deletion_requested_at);

ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS fk_tx_user;

ALTER TABLE transactions
    ADD CONSTRAINT fk_tx_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE external_portfolios
    DROP CONSTRAINT IF EXISTS fk_external_portfolios_user;

ALTER TABLE external_portfolios
    ADD CONSTRAINT fk_external_portfolios_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE external_position_lots
    DROP CONSTRAINT IF EXISTS fk_external_position_lots_portfolio;

ALTER TABLE external_position_lots
    ADD CONSTRAINT fk_external_position_lots_portfolio
        FOREIGN KEY (portfolio_id)
            REFERENCES external_portfolios (id)
            ON DELETE CASCADE;

ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS fk_transactions_external_portfolio;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_external_portfolio
        FOREIGN KEY (external_portfolio_id)
            REFERENCES external_portfolios (id)
            ON DELETE CASCADE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'alarm_history'
          AND constraint_name = 'fk_alarm_history_user'
    ) THEN
        ALTER TABLE alarm_history
            ADD CONSTRAINT fk_alarm_history_user
                FOREIGN KEY (user_id)
                    REFERENCES users (id)
                    ON DELETE CASCADE;
    END IF;
END $$;

