create table watchlist_items (
    id bigserial primary key,
    user_id uuid not null,
    instrument_id bigint not null,
    active boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_watchlist_user
        foreign key (user_id)
            references users(id)
            on delete cascade,
    constraint fk_watchlist_instrument
        foreign key (instrument_id)
            references instruments(id)
            on delete cascade,
    constraint uq_watchlist_user_instrument
        unique (user_id, instrument_id)
);

create index idx_watchlist_user
    on watchlist_items (user_id);

create index idx_watchlist_instrument
    on watchlist_items (instrument_id);

create index idx_watchlist_active
    on watchlist_items (active);
