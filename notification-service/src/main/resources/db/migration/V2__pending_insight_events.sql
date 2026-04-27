create table pending_insight_events (
    id bigserial primary key,
    user_id uuid not null,
    instrument_id bigint not null,
    symbol varchar(64) not null,
    change_percent numeric(20,8) not null,
    direction varchar(8) not null,
    occurred_at timestamp not null,
    processed boolean not null
);

create index idx_pending_insight_events_user_id
    on pending_insight_events (user_id);

create index idx_pending_insight_events_processed
    on pending_insight_events (processed);
