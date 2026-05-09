alter table portfolio_snapshots
    add column external_portfolio_id bigint null
        constraint fk_portfolio_snapshots_external_portfolio
            references external_portfolios (id)
            on delete cascade;

create index idx_portfolio_snapshots_user_portfolio_created
    on portfolio_snapshots (user_id, external_portfolio_id, created_at);
