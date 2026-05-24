CREATE TABLE blocked_registration_emails (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    blocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source_user_id UUID,
    CONSTRAINT uq_blocked_registration_emails_email UNIQUE (email)
);

CREATE INDEX idx_blocked_registration_emails_blocked_at ON blocked_registration_emails (blocked_at DESC);
