ALTER TABLE users
    ADD COLUMN frozen_at TIMESTAMPTZ,
    ADD COLUMN frozen_reason VARCHAR(500);

ALTER TABLE alarm_history
    ALTER COLUMN instrument_symbol DROP NOT NULL,
    ALTER COLUMN condition DROP NOT NULL,
    ALTER COLUMN price DROP NOT NULL;

ALTER TABLE alarm_history
    ADD COLUMN title VARCHAR(200),
    ADD COLUMN body TEXT;
