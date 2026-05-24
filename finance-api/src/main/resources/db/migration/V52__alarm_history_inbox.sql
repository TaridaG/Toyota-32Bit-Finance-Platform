ALTER TABLE alarm_history
    ADD COLUMN IF NOT EXISTS notification_type VARCHAR(32) NOT NULL DEFAULT 'ALARM',
    ADD COLUMN IF NOT EXISTS threshold NUMERIC(19, 6),
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_alarm_history_user_inbox
    ON alarm_history (user_id, deleted, triggered_at DESC);
