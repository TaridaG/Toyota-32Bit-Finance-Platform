create table portfolio_daily_performance (
    id bigserial primary key,
    user_id uuid not null,
    external_portfolio_id bigint not null
        constraint fk_portfolio_daily_performance_portfolio
            references external_portfolios (id)
            on delete cascade,
    currency varchar(8) not null,
    day_utc date not null,
    market_value numeric(19, 6) not null,
    net_flow numeric(19, 6) not null,
    daily_pnl numeric(19, 6) not null,
    daily_return_pct numeric(19, 6) null,
    twr_index numeric(19, 10) not null,
    complete boolean not null default true,
    computed_at timestamptz not null
);

create unique index uq_portfolio_daily_performance_key
    on portfolio_daily_performance (user_id, external_portfolio_id, currency, day_utc);

create index idx_portfolio_daily_performance_portfolio_currency_day
    on portfolio_daily_performance (external_portfolio_id, currency, day_utc);

create index idx_portfolio_daily_performance_user_currency_day
    on portfolio_daily_performance (user_id, currency, day_utc);
