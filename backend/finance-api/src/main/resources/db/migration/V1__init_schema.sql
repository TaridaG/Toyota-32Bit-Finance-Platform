-- =========================
-- USERS
-- =========================
create extension if not exists pgcrypto;
create table users (
                       id uuid primary key default gen_random_uuid(),
                       email varchar(255) not null unique,
                       username varchar(255) not null unique,
                       active boolean not null,
                       created_at timestamptz not null
);

-- =========================
-- INSTRUMENTS
-- =========================
create table instruments (
                             id bigserial primary key,
                             symbol varchar(50) not null unique,
                              name varchar(255) not null,
                             type varchar(50) not null,
                             exchange varchar(50) not null,
                             active boolean not null
);

-- =========================
-- INSTRUMENT PRICES
-- =========================
create table instrument_prices (
                                   id bigserial primary key,
                                   instrument_id bigint not null,
                                   price_type varchar(50) not null,
                                   price numeric(19,6) not null,
                                   timestamp timestamptz not null,

                                   constraint fk_price_instrument
                                       foreign key (instrument_id)
                                           references instruments(id)
                                           on delete cascade
);

create index idx_price_instrument_time
    on instrument_prices (instrument_id, timestamp);

create index idx_price_timestamp
    on instrument_prices (timestamp);

-- =========================
-- ALARM RULES
-- =========================
create table alarm_rules (
                             id bigserial primary key,
                             user_id uuid not null,
                             instrument_id bigint not null,
                             condition varchar(50) not null,
                             threshold numeric(19,6) not null,
                             active boolean not null,
                             created_at timestamptz not null,

                             constraint fk_alarm_user
                                 foreign key (user_id)
                                     references users(id)
                                     on delete cascade,

                             constraint fk_alarm_instrument
                                 foreign key (instrument_id)
                                     references instruments(id)
                                     on delete cascade
);

create index idx_alarm_user
    on alarm_rules (user_id);

create index idx_alarm_instrument
    on alarm_rules (instrument_id);

create index idx_alarm_active
    on alarm_rules (active);

-- =========================
-- DEMO BALANCES
-- =========================
create table demo_balances (
                               id uuid primary key,
                               user_id uuid not null unique,
                               balance numeric(19,4) not null,
                               currency varchar(10) not null,
                               created_at timestamptz not null,

                               constraint fk_demo_balance_user
                                   foreign key (user_id)
                                       references users(id)
                                       on delete cascade
);