CREATE TABLE admin_portal_roster_counters (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    deleted_accounts_total BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO admin_portal_roster_counters (id, deleted_accounts_total, updated_at)
VALUES (1, 0, NOW());
