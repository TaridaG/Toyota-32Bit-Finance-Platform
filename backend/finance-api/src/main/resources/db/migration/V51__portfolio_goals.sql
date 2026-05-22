CREATE TABLE portfolio_goals (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    external_portfolio_id BIGINT       NULL REFERENCES external_portfolios (id) ON DELETE CASCADE,
    goal_type           VARCHAR(32)  NOT NULL,
    profit_target_mode  VARCHAR(16)  NULL,
    target_amount       NUMERIC(19, 4) NULL,
    target_percent      NUMERIC(9, 4) NULL,
    title               VARCHAR(200) NOT NULL DEFAULT '',
    description         VARCHAR(2000) NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_portfolio_goals_user_scope_type
    ON portfolio_goals (user_id, goal_type, COALESCE(external_portfolio_id, -1));

CREATE INDEX idx_portfolio_goals_user ON portfolio_goals (user_id);
