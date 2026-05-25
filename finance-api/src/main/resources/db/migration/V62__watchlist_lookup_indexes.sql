create index if not exists idx_watchlist_user_active
    on watchlist_items (user_id, active);
