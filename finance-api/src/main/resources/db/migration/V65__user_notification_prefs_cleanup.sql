ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notify_watchlist_alerts boolean NOT NULL DEFAULT true;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notify_alarm_alerts boolean NOT NULL DEFAULT true;

ALTER TABLE users
    DROP COLUMN IF EXISTS notify_product_updates;
