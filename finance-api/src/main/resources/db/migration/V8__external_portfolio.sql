create table external_portfolios (
    id bigserial primary key,
    user_id uuid not null,
    name varchar(120) not null,
    base_currency varchar(3) not null default 'TRY',
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_external_portfolios_user
        foreign key (user_id) references users(id)
);

create table external_position_lots (
    id bigserial primary key,
    portfolio_id bigint not null,
    instrument_id bigint not null,
    quantity numeric(19,8) not null,
    unit_price numeric(19,8) not null,
    fee_amount numeric(19,8),
    fee_currency varchar(3),
    acquired_at timestamp not null,
    source_type varchar(30) not null,
    source_name varchar(120),
    notes varchar(500),
    is_deleted boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    version bigint not null default 0,

    constraint fk_external_position_lots_portfolio
        foreign key (portfolio_id) references external_portfolios(id),

    constraint fk_external_position_lots_instrument
        foreign key (instrument_id) references instruments(id),

    constraint chk_external_position_lots_quantity_positive
        check (quantity > 0),

    constraint chk_external_position_lots_unit_price_positive
        check (unit_price > 0),

    constraint chk_external_position_lots_fee_amount_non_negative
        check (fee_amount is null or fee_amount >= 0)
);

create index idx_external_portfolios_user_id
    on external_portfolios(user_id);

create index idx_external_position_lots_portfolio_id
    on external_position_lots(portfolio_id);

create index idx_external_position_lots_instrument_id
    on external_position_lots(instrument_id);

create index idx_external_position_lots_portfolio_active
    on external_position_lots(portfolio_id, is_deleted);
