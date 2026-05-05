ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_avatar_updated_at TIMESTAMPTZ;
