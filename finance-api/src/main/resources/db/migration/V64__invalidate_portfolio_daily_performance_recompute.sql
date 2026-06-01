ALTER TABLE users
    ADD COLUMN IF NOT EXISTS notify_watchlist_alerts boolean NOT NULL DEFAULT true;
