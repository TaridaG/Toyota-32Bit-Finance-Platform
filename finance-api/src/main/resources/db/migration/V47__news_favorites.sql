create table news_favorites (
    id bigserial primary key,
    user_id uuid not null,
    news_id bigint not null,
    active boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_news_favorites_user
        foreign key (user_id)
            references users (id)
            on delete cascade,
    constraint uq_news_favorites_user_news
        unique (user_id, news_id)
);

create index idx_news_favorites_user
    on news_favorites (user_id);

create index idx_news_favorites_news
    on news_favorites (news_id);

create index idx_news_favorites_active
    on news_favorites (active);
