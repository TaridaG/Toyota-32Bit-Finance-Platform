alter table pending_insight_events drop constraint uq_pending_insight_dedup;

create unique index uq_pending_insight_analytics
    on pending_insight_events (user_id, instrument_id, direction, occurred_at)
    where event_type = 'INSIGHT';

create unique index uq_pending_news_per_user_instrument_title
    on pending_insight_events (user_id, instrument_id, news_title)
    where event_type = 'NEWS' and news_title is not null;
