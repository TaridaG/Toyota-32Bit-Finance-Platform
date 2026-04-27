create table watchlist_projection (
    id bigserial primary key,
    user_id uuid not null,
    instrument_id bigint not null,
    symbol varchar(64) not null,
    active boolean not null,
    updated_at timestamp not null,
    constraint uq_watchlist_projection_user_instrument unique (user_id, instrument_id)
);

create index idx_watchlist_projection_instrument_active
    on watchlist_projection (instrument_id, active);

create index idx_watchlist_projection_user_active
    on watchlist_projection (user_id, active);

create table notification_processed_events (
    event_id uuid primary key,
    event_type varchar(128) not null,
    processed_at timestamp not null
);

create index idx_notification_processed_events_processed_at
    on notification_processed_events (processed_at);

create table notification_delivery_state (
    id bigserial primary key,
    user_id uuid not null,
    instrument_id bigint not null,
    insight_type varchar(128) not null,
    last_sent_at timestamp not null,
    last_value numeric(20,8),
    updated_at timestamp not null,
    constraint uq_notification_delivery_state_key unique (user_id, instrument_id, insight_type)
);
