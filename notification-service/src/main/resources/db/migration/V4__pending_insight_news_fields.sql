alter table pending_insight_events
    add column event_type varchar(16) not null default 'INSIGHT';

alter table pending_insight_events
    add column news_title varchar(500);

alter table pending_insight_events
    alter column change_percent drop not null;
