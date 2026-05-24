ALTER TABLE users
    ADD COLUMN IF NOT EXISTS totp_secret_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS totp_enabled_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS login_mfa_challenge (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_login_mfa_challenge_expires ON login_mfa_challenge (expires_at);
