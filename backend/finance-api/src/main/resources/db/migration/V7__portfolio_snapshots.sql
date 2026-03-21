create table portfolio_snapshots (
                                     id bigserial primary key,
                                     user_id uuid not null,
                                     total_cost numeric(19,6) not null,
                                     total_value numeric(19,6) not null,
                                     unrealized_pnl numeric(19,6) not null,
                                     created_at timestamptz not null default now(),

                                     constraint fk_portfolio_snapshots_user
                                         foreign key (user_id) references users(id) on delete cascade
);

create index idx_portfolio_snapshots_user_created_at
    on portfolio_snapshots (user_id, created_at);