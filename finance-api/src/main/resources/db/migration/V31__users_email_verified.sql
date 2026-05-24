-- Portal admin: filter by verified email (registration flow always verifies before row exists; default true for legacy rows).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL;
