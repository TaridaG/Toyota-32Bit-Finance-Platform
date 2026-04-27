alter table pending_insight_events
    add constraint uq_pending_insight_dedup
        unique (user_id, instrument_id, direction, occurred_at);
