create table if not exists mds_evds_tl_deposit_daily_index (
    id bigserial primary key,
    day date not null,
    maturity_code varchar(16) not null,
    index_value numeric(19,10) not null,
    annual_rate_percent numeric(12,4) not null,
    source_observation_date date,
    source_provider varchar(32) not null,
    computed_at timestamptz not null,
    constraint uk_mds_tl_dep_daily_idx_day_maturity unique (day, maturity_code)
);

create index if not exists idx_mds_tl_dep_daily_idx_maturity_day
    on mds_evds_tl_deposit_daily_index (maturity_code, day);
