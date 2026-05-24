CREATE TABLE IF NOT EXISTS trusted_login_device (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_trusted_login_device_user ON trusted_login_device (user_id);
CREATE INDEX IF NOT EXISTS idx_trusted_login_device_expires ON trusted_login_device (expires_at);
